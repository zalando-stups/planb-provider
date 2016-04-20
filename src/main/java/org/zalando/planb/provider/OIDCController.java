package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.bind.annotation.*;
import org.zalando.planb.provider.realms.*;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.zalando.planb.provider.Metric.trimSlash;

@RestController
@Slf4j
public class OIDCController {

    private static final Joiner COMMA_SEPARATED = Joiner.on(",");

    private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

    private static final String BASIC_AUTH_PREFIX = "Basic ";

    @Autowired
    private RealmConfig realms;

    @Autowired
    private RealmProperties realmProperties;

    @Autowired
    private OIDCKeyHolder keyHolder;

    @Autowired
    private JWTIssuer jwtIssuer;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private ScopeService scopeService;

    @Autowired
    private CassandraAuthorizationCodeService cassandraAuthorizationCodeService;

    /**
     * Get client_id and client_secret from HTTP Basic Auth
     */
    public static ClientCredentials getClientCredentials(Optional<String> authorization) throws RealmAuthenticationException {
        String[] basicAuth = authorization
                .filter(string -> string.toUpperCase().startsWith(BASIC_AUTH_PREFIX.toUpperCase()))
                .map(string -> string.substring(BASIC_AUTH_PREFIX.length()))
                .map(BASE_64_DECODER::decode)
                .map(bytes -> new String(bytes, UTF_8))
                .map(string -> string.split(":", 2))
                .filter(array -> array.length == 2)
                .orElseThrow(() -> new BadRequestException(
                        "Malformed or missing Authorization header.",
                        "invalid_client",
                        "Client authentication failed"));
        return ClientCredentials.builder().clientId(basicAuth[0]).clientSecret(basicAuth[1]).build();
    }

    public static ClientCredentials getClientCredentials(Optional<String> authorization, Optional<String> clientId, Optional<String> clientSecret) throws RealmAuthenticationException {
        if (clientId.isPresent() && clientSecret.isPresent()) {
            return ClientCredentials.builder().clientId(clientId.get()).clientSecret(clientSecret.get()).build();
        } else {
            return getClientCredentials(authorization);
        }
    }

    static String getRealmName(RealmConfig realms, Optional<String> realmNameParam, Optional<String> hostHeader) {
        return realmNameParam.orElseGet(() -> realms.findRealmNameInHost(hostHeader
                .orElseThrow(() -> new BadRequestException("Missing realm parameter and no Host header.", "missing_realm", "Missing realm parameter and no Host header.")))
                .orElseThrow(() -> new RealmNotFoundException(hostHeader.get())));
    }

    static String getRealmName(RealmConfig realms, String realmNameParam) {
        return  realms.findRealmNameInRealm(realmNameParam)
                .orElseThrow(() -> new RealmNotFoundException(realmNameParam));
    }

    private OIDCCreateTokenResponse response(String accessToken, Set<String> scopes, String realmName) {
        return OIDCCreateTokenResponse.builder()
                .accessToken(accessToken)
                .idToken(scopes.contains("openid") ? accessToken : (String)null)
                .expiresIn(realmProperties.getTokenLifetime(realmName).getSeconds())
                .scope(ScopeService.join(scopes))
                .realm(realmName)
                .tokenType(OAuth2AccessToken.BEARER_TYPE)
                .build();
    }

    /**
     * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
     */
    @RequestMapping(value = "/oauth2/access_token", method = RequestMethod.POST, params = "grant_type=authorization_code")
    @ResponseBody
    OIDCCreateTokenResponse createTokenFromCode(
            @RequestParam(value = "grant_type", required = true) String grantType,
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "client_id") Optional<String> clientIdParam,
            @RequestParam(value = "client_secret") Optional<String> clientSecretParam,
            @RequestParam(value = "redirect_uri", required = true) URI redirectUri,
            @RequestHeader(name = "Authorization") Optional<String> authorization) throws JOSEException {

        final Metric metric = new Metric(metricRegistry).start();
        final AuthorizationCode authCode = cassandraAuthorizationCodeService.invalidate(code)
                .orElseThrow(() -> new BadRequestException("Invalid authorization code", "invalid_request", "Invalid authorization code"));

        // Check that redirect_uri parameter matches the one from authorization request
        // (required by RFC, see http://tools.ietf.org/html/rfc6749#section-4.1.3
        // In order to prevent such an attack, the authorization server MUST
        // ensure that the redirection URI used to obtain the authorization code
        // is identical to the redirection URI provided when exchanging the
        // authorization code for an access token.
        if (!redirectUri.equals(authCode.getRedirectUri())) {
            throw new BadRequestException("Invalid authorization code: redirect_uri mismatch", "invalid_request", "Invalid authorization code: redirect_uri mismatch");
        }

        final String realmName = authCode.getRealm();
        try {

            // retrieve realms for the given realm
            ClientRealm clientRealm = realms.getClientRealm(realmName);
            UserRealm userRealm = realms.getUserRealm(realmName);

            final ClientCredentials clientCredentials = getClientCredentials(authorization, clientIdParam, clientSecretParam);
            clientRealm.authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), authCode.getScopes(), authCode.getScopes());

            if (!clientCredentials.getClientId().equals(authCode.getClientId())) {
                // authorization code can only be used by the client who requested it
                throw new BadRequestException("Invalid authorization code: client mismatch", "invalid_request", "Invalid authorization code: client mismatch");
            }

            final String rawJWT = jwtIssuer.issueAccessToken(userRealm, clientCredentials.getClientId(), authCode.getScopes(), authCode.getClaims());
            metric.finish("planb.provider.access_token." + trimSlash(realmName) + ".success");

            return response(rawJWT, authCode.getScopes(), realmName);
        } catch (Throwable t) {
            final String errorType = Optional.of(t)
                    .filter(e -> e instanceof RestException)
                    .map(e -> (RestException) e)
                    .flatMap(RestException::getErrorLocation)
                    .orElse("other");
            metric.finish("planb.provider.access_token." + trimSlash(realmName) + ".error." + errorType);
            throw t;
        }
    }

    /**
     * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
     */
    @RequestMapping(value = {"/oauth2/access_token", "/z/oauth2/access_token"}, method = RequestMethod.POST, params = "grant_type=password")
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "realm") Optional<String> realmNameParam,
                                        @RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,

                                        @RequestParam(value = "scope") Optional<String> scope,
                                        @RequestParam(value = "client_id") Optional<String> clientIdParam,
                                        @RequestParam(value = "client_secret") Optional<String> clientSecretParam,
                                        @RequestHeader(name = "Authorization") Optional<String> authorization,
                                        @RequestHeader(name = "Host") Optional<String> hostHeader)
            throws RealmAuthenticationException, RealmAuthorizationException, JOSEException {
        final Metric metric = new Metric(metricRegistry).start();

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        try {
            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                throw new BadRequestException(
                        "Username and password should be provided.",
                        "invalid_grant",
                        "The provided access grant is invalid, expired, or revoked.");
            }

            // retrieve realms for the given realm
            ClientRealm clientRealm = realms.getClientRealm(realmName);
            UserRealm userRealm = realms.getUserRealm(realmName);
            final ClientCredentials clientCredentials = getClientCredentials(authorization, clientIdParam, clientSecretParam);

            // parse requested scopes
            final Set<String> scopes = ScopeService.split(scope);
            final Set<String> defaultScopes = scopeService.getDefaultScopesForClient(clientRealm, clientCredentials.getClientId());
            final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;

            clientRealm.authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), finalScopes, defaultScopes);
            final Map<String, String> extraClaims = userRealm.authenticate(username, password, finalScopes, defaultScopes);

            // request authorized, create JWT
            final String rawJWT = jwtIssuer.issueAccessToken(userRealm, clientCredentials.getClientId(), finalScopes, extraClaims);
            metric.finish("planb.provider.access_token." + trimSlash(realmName) + ".success");

            return response(rawJWT, finalScopes, realmName);
        } catch (Throwable t) {
            final String errorType = Optional.of(t)
                    .filter(e -> e instanceof RestException)
                    .map(e -> (RestException) e)
                    .flatMap(RestException::getErrorLocation)
                    .orElse("other");
            metric.finish("planb.provider.access_token." + trimSlash(realmName) + ".error." + errorType);
            throw t;
        }
    }

    @RequestMapping("/.well-known/openid-configuration")
    OIDCDiscoveryInformationResponse getDiscoveryInformation(
            @RequestHeader(name = "Host") String hostname,
            @RequestHeader(name = "X-Forwarded-Proto", required = false) String proto) {
        if (proto == null) {
            proto = "http";
        }

        return new OIDCDiscoveryInformationResponse(proto, hostname);
    }

    @RequestMapping(value = OIDCDiscoveryInformationResponse.KEYS_PATH, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    String getSigningKeys() {
        List<String> jwks = keyHolder.getCurrentPublicKeys().stream()
                .map(JWK::toJSONString)
                .collect(Collectors.toList());

        return "{\"keys\": [" + COMMA_SEPARATED.join(jwks) + "]}";
    }
}
