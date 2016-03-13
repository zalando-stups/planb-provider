package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JOSEException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.zalando.planb.provider.realms.ClientRealm;
import org.zalando.planb.provider.realms.UserRealm;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static org.zalando.planb.provider.realms.ClientRealmAuthenticationException.clientNotFound;
import static org.zalando.planb.provider.OIDCController.getRealmName;
import static org.zalando.planb.provider.OIDCController.validateRedirectUri;

@Controller
public class AuthorizeController {

    static final Set<String> SUPPORTED_RESPONSE_TYPES = ImmutableSet.of("code", "token");

    @Autowired
    private RealmConfig realms;

    @Autowired
    private ScopeProperties scopeProperties;

    @Autowired
    private JWTIssuer jwtIssuer;

    @Autowired
    private CassandraAuthorizationCodeService cassandraAuthorizationCodeService;

    /**
     * Authorization Request, see http://tools.ietf.org/html/rfc6749#section-4.1.1
     */
    @RequestMapping(value = "/oauth2/authorize")
    String showAuthorizationForm(@RequestParam(value = "realm") Optional<String> realmNameParam,
                                 @RequestParam(value = "response_type", required = true) String responseType,
                                 @RequestParam(value = "client_id", required = true) String clientId,
                                 @RequestParam(value = "scope") Optional<String> scope,
                                 @RequestParam(value = "redirect_uri") Optional<URI> redirectUriParam,
                                 @RequestParam(value = "state") Optional<String> state,
                                 @RequestHeader(value = "Host") Optional<String> hostHeader,
                                 Model model) {

        if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {
            throw new BadRequestException("Invalid response_type", "invalid_request", "Invalid response_type");
        }

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        ClientRealm clientRealm = realms.getClientRealm(realmName);

        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        // Either use passed Redirect URI or get configured Redirect URI
        // "redirect_uri" parameter is OPTIONAL according to http://tools.ietf.org/html/rfc6749#section-4.1.1
        // NOTE 1: we use "findFirst", i.e. if no parameter was passed and the client has multiple Redirect URIs configured, it will take a "random" one
        // NOTE 2: not passing the "redirect_uri" parameter allows "snooping" the configured Redirect URI(s) for known client IDs --- client IDs should be unpredictable to prevent this
        final URI redirectUri = redirectUriParam
                .orElseGet(() -> clientData.getRedirectUris().stream().findFirst().map(URI::create)
                        .orElseThrow(() -> new BadRequestException("Missing redirect_uri", "invalid_request", "Missing redirect_uri")));

        validateRedirectUri(realmName, clientId, clientData, redirectUri);

        model.addAttribute("responseType", responseType);
        model.addAttribute("realm", realmName);
        model.addAttribute("clientId", clientId);
        model.addAttribute("scope", scope.orElse(""));
        model.addAttribute("state", state.orElse(""));
        model.addAttribute("redirectUri", redirectUri.toString());

        return "login";
    }

    @RequestMapping(value = "/oauth2/authorize", method = RequestMethod.POST)
    void authorize(
            @RequestParam(value = "response_type", required = true) String responseType,
            @RequestParam(value = "realm") Optional<String> realmNameParam,
            @RequestParam(value = "client_id", required = true) String clientId,
            @RequestParam(value = "scope") Optional<String> scope,
            @RequestParam(value = "redirect_uri") URI redirectUri,
            @RequestParam(value = "state") Optional<String> state,
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "password", required = true) String password,
            @RequestHeader(value = "Host") Optional<String> hostHeader,
            HttpServletResponse response) throws IOException, URISyntaxException, JOSEException {

        if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {
            throw new BadRequestException("Invalid response_type", "invalid_request", "Invalid response_type");
        }

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        // retrieve realms for the given realm
        ClientRealm clientRealm = realms.getClientRealm(realmName);

        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        if ("token".equals(responseType) && clientData.isConfidential()) {
            throw new BadRequestException(
                    format("Invalid response_type 'token' for confidential client %s", clientId),
                    "invalid_request", "Invalid response_type 'token' for confidential client");
        }

        // make sure (again!) that the redirect_uri was configured in the client
        validateRedirectUri(realmName, clientId, clientData, redirectUri);

        final Set<String> scopes = ScopeProperties.split(scope);
        final Set<String> defaultScopes = scopeProperties.getDefaultScopes(realmName);
        final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;

        UserRealm userRealm = realms.getUserRealm(realmName);

        final Map<String, String> claims = userRealm.authenticate(username, password, finalScopes, defaultScopes);

        URI redirect;
        if ("code".equals(responseType)) {

            final String code = cassandraAuthorizationCodeService.create(state.orElse(""), clientId, realmName, finalScopes, claims, redirectUri);

            redirect = new URIBuilder(redirectUri).addParameter("code", code).addParameter("state", state.orElse("")).build();
        } else {
            String rawJWT = jwtIssuer.issueAccessToken(userRealm, clientId, finalScopes, claims);
            redirect = new URIBuilder(redirectUri).addParameter("access_token", rawJWT)
                    .addParameter("token_type", "Bearer")
                    .addParameter("expires_in", String.valueOf(JWTIssuer.EXPIRATION_TIME.getSeconds()))
                    .addParameter("scope", ScopeProperties.join(scopes))
                    .addParameter("state", state.orElse("")).build();
        }
        response.sendRedirect(redirect.toString());
    }
}
