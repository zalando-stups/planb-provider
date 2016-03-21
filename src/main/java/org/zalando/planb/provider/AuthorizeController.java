package org.zalando.planb.provider;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import static org.zalando.planb.provider.OIDCController.getRealmName;
import static org.zalando.planb.provider.OIDCController.validateRedirectUri;
import static org.zalando.planb.provider.realms.ClientRealmAuthenticationException.clientNotFound;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.http.client.utils.URIBuilder;

import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;

import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import org.zalando.planb.provider.realms.ClientRealm;
import org.zalando.planb.provider.realms.UserRealm;
import org.zalando.planb.provider.realms.UserRealmAuthenticationException;

import com.google.common.collect.ImmutableSet;

import com.nimbusds.jose.JOSEException;

@Controller
@RequestMapping(path = AuthorizeController.AUTHORIZE_ENDPOINT)
public class AuthorizeController {

    // TODO: extract to a constant class
    private static final String ACCESS_DENIED_ERROR = "access_denied";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String ALLOW = "allow";
    private static final String BEARER = "Bearer";
    private static final String CLIENT_DESCRIPTION = "clientDescription";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_ID_SNAKECASE = "client_id";
    private static final String CLIENT_NAME = "clientName";
    private static final String CODE = "code";
    private static final String CONSENT = "consent";
    private static final String DECISION = "decision";
    private static final String DEFAULT_DECISION = "none";
    private static final String DENY = "deny";
    private static final String EMPTY = "";
    private static final String ERROR = "error";
    private static final String EXPIRES_IN = "expires_in";
    private static final String HOST = "Host";
    private static final String PASSWORD = "password";
    private static final String REALM = "realm";
    private static final String REDIRECT_URI = "redirectUri";
    private static final String REDIRECT_URI_SNAKECASE = "redirect_uri";
    private static final String RESPONSE_TYPE = "responseType";
    private static final String RESPONSE_TYPE_SNAKECASE = "response_type";
    private static final String SCOPE = "scope";
    private static final String SCOPES = "scopes";
    private static final String STATE = "state";
    private static final String TOKEN = "token";
    private static final String TOKEN_TYPE = "token_type";
    private static final String USERNAME = "username";
    private static final String LOGIN_FORM = "login";

    private final Logger log = getLogger(getClass());

    static final Set<String> SUPPORTED_RESPONSE_TYPES = ImmutableSet.of(CODE, TOKEN);
    static final String AUTHORIZE_ENDPOINT = "/oauth2/authorize";

    @Autowired
    private RealmConfig realms;

    @Autowired
    private ScopeProperties scopeProperties;

    @Autowired
    private JWTIssuer jwtIssuer;

    @Autowired
    private CassandraAuthorizationCodeService cassandraAuthorizationCodeService;

    @Autowired
    private CassandraConsentService cassandraConsentService;

    /**
     * Authorization Request, see http://tools.ietf.org/html/rfc6749#section-4.1.1.
     */
    @RequestMapping
    String showAuthorizationForm(@RequestParam(value = REALM) final Optional<String> realmNameParam,
            @RequestParam(value = RESPONSE_TYPE_SNAKECASE, required = true) final String responseType,
            @RequestParam(value = CLIENT_ID_SNAKECASE, required = true) final String clientId,
            @RequestParam(value = SCOPE) final Optional<String> scope,
            @RequestParam(value = REDIRECT_URI_SNAKECASE) final Optional<URI> redirectUriParam,
            @RequestParam(value = STATE) final Optional<String> state,
            @RequestParam(value = ERROR) final Optional<String> error,
            @RequestHeader(value = HOST) final Optional<String> hostHeader, final Model model) {

        if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {

            // see https://tools.ietf.org/html/rfc6749#section-4.2.2.1
            throw new BadRequestException("Unsupported response_type", "unsupported_response_type",
                "Unsupported response_type");
        }

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        ClientRealm clientRealm = realms.getClientRealm(realmName);

        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        // Either use passed Redirect URI or get configured Redirect URI
        // "redirect_uri" parameter is OPTIONAL according to http://tools.ietf.org/html/rfc6749#section-4.1.1
        // NOTE 1: we use "findFirst", i.e. if no parameter was passed and the client has multiple Redirect URIs
        // configured, it will take a "random" one
        // NOTE 2: not passing the "redirect_uri" parameter allows "snooping" the configured Redirect URI(s) for known
        // client IDs --- client IDs should be unpredictable to prevent this
        final URI redirectUri = redirectUriParam
                .orElseGet(() ->
                                clientData
                                .getRedirectUris()
                                .stream()
                                .findFirst()
                                .map(URI::create)
                                .orElseThrow(() ->
                                                  new BadRequestException("Missing redirect_uri",
                                                          "invalid_request", "Missing redirect_uri")));

        validateRedirectUri(realmName, clientId, clientData, redirectUri);

        updateModelForLogin(model, responseType, realmName, clientId, scope, state, redirectUri, error);

        return LOGIN_FORM;
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    AuthorizeResponse authorizeAsJson(
            @RequestParam(value = RESPONSE_TYPE_SNAKECASE, required = true) final String responseType,
            @RequestParam(value = REALM) final Optional<String> realmNameParam,
            @RequestParam(value = CLIENT_ID_SNAKECASE, required = true) final String clientId,
            @RequestParam(value = SCOPE) final Optional<String> scope,
            @RequestParam(value = REDIRECT_URI_SNAKECASE) final URI redirectUri,
            @RequestParam(value = STATE) final Optional<String> state,
            @RequestParam(value = USERNAME, required = true) final String username,
            @RequestParam(value = PASSWORD, required = true) final String password,
            @RequestParam(value = DECISION) final Optional<String> decision,
            @RequestHeader(value = HOST) final Optional<String> hostHeader) throws IOException, URISyntaxException,
        JOSEException {

        if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {
            throw new BadRequestException("Unsupported response_type", "unsupported_response_type",
                "Unsupported response_type");
        }

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        // retrieve realms for the given realm
        final ClientRealm clientRealm = realms.getClientRealm(realmName);
        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        if (tokenResponseForConfidentialClient(responseType, clientData)) {
            throw new BadRequestException(format("Invalid response_type 'token' for confidential client %s", clientId),
                "invalid_request", "Invalid response_type 'token' for confidential client");
        }

        // make sure (again!) that the redirect_uri was configured in the client
        validateRedirectUri(realmName, clientId, clientData, redirectUri);

        final Set<String> scopes = ScopeProperties.split(scope);
        final Set<String> defaultScopes = scopeProperties.getDefaultScopes(realmName);
        final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;
        final UserRealm userRealm = realms.getUserRealm(realmName);
        final AuthorizeResponse authorizeResponse = new AuthorizeResponse();

        URI redirect;

        try {
            final Map<String, String> claims = userRealm.authenticate(username, password, finalScopes, defaultScopes);
            final Optional<Set<String>> consentedScopes = storeOrGetConsentedScopes(decision, username, clientId,
                    userRealm, finalScopes);

            // no consented scopes -> access was denied
            if (!consentedScopes.isPresent()) {
                return noConsentedScopesResponse(redirectUri, state);
            }

            if (!allScopesAreConsented(consentedScopes, finalScopes)) {

                // return JSON object with "scopes" property if "Accept" header specifies "application/json"
                // see http://tools.ietf.org/html/rfc7231#section-5.3.2
                return generateConsentNeededResponse(clientData, finalScopes, realmName, clientId, redirectUri,
                        username, password, responseType, state);
            }

            // generate scope based on grant type
            if (isAuthorizationCodeGrant(responseType)) {
                redirect = generateCodeResponseURI(realmName, state, clientId, finalScopes, claims, redirectUri);
            } else { // implicit grant

                // see http://tools.ietf.org/html/rfc7231#section-5.3.2
                redirect = generateTokenResponseURI(userRealm, state, clientId, finalScopes, claims, redirectUri);
            }
        } catch (UserRealmAuthenticationException e) {

            // redirect back to login form with error message
            log.info("{} (status {} / {})", e.getMessage(), e.getStatusCode(), e.getClass().getSimpleName());
            redirect = generateLoginFormURI(realmName, responseType, state, clientId, finalScopes, redirectUri);
        }

        authorizeResponse.setRedirect(redirect.toString());
        return authorizeResponse;
    }

    @RequestMapping(method = RequestMethod.POST)
    ModelAndView authorizeAsModel(
            @RequestParam(value = RESPONSE_TYPE_SNAKECASE, required = true) final String responseType,
            @RequestParam(value = REALM) final Optional<String> realmNameParam,
            @RequestParam(value = CLIENT_ID_SNAKECASE, required = true) final String clientId,
            @RequestParam(value = SCOPE) final Optional<String> scope,
            @RequestParam(value = REDIRECT_URI_SNAKECASE) final URI redirectUri,
            @RequestParam(value = STATE) final Optional<String> state,
            @RequestParam(value = USERNAME, required = true) final String username,
            @RequestParam(value = PASSWORD, required = true) final String password,
            @RequestParam(value = DECISION) final Optional<String> decision,
            @RequestHeader(value = HOST) final Optional<String> hostHeader) throws IOException, URISyntaxException,
        JOSEException {
        final AuthorizeResponse authorizeResponse = authorizeAsJson(responseType, realmNameParam, clientId, scope,
                redirectUri, state, username, password, decision, hostHeader);
        ModelAndView modelAndView;

        if (authorizeResponse.isConsentNeeded()) {
            modelAndView = generateConsentScopesView(authorizeResponse);
        } else {
            modelAndView = new ModelAndView(new RedirectView(authorizeResponse.getRedirect()));
        }

        return modelAndView;
    }

    private boolean allScopesAreConsented(final Optional<Set<String>> consentedScopes, final Set<String> finalScopes) {
        return consentedScopes.get().containsAll(finalScopes);
    }

    private boolean tokenResponseForConfidentialClient(final String responseType, final ClientData clientData) {
        return isImplicitGrant(responseType) && clientData.isConfidential();
    }

    private boolean isAuthorizationCodeGrant(final String responseType) {
        return CODE.equals(responseType);
    }

    private boolean isImplicitGrant(final String responseType) {
        return TOKEN.equals(responseType);
    }

    private Optional<Set<String>> storeOrGetConsentedScopes(final Optional<String> decision, final String username,
            final String clientId, final UserRealm userRealm, final Set<String> finalScopes) throws URISyntaxException {
        Optional<Set<String>> consentedScopes = Optional.empty();

        switch (decision.orElse(DEFAULT_DECISION)) {

            case ALLOW :
                // save user consent
                cassandraConsentService.store(username, userRealm.getName(), clientId, finalScopes);
                consentedScopes = Optional.of(finalScopes);
                break;

            case DENY :
                // redirect user to callback URL if decision is "deny" (error=access_denied)
                // see http://tools.ietf.org/html/rfc6749#section-4.1.2.1
                break;

            default :
                consentedScopes = Optional.of(cassandraConsentService.getConsentedScopes(username, userRealm.getName(),
                            clientId));
                break;
        }

        return consentedScopes;
    }

    private ModelAndView generateConsentScopesView(final AuthorizeResponse authorizeResponse) {
        final Map<String, Object> model = new HashMap<>();
        model.put(CLIENT_NAME, authorizeResponse.getClientName());
        model.put(CLIENT_DESCRIPTION, authorizeResponse.getClientDescription());
        model.put(SCOPES, authorizeResponse.getScopes());
        model.put(RESPONSE_TYPE, authorizeResponse.getResponseType());
        model.put(REALM, authorizeResponse.getRealm());
        model.put(CLIENT_ID, authorizeResponse.getClientId());
        model.put(SCOPE, authorizeResponse.getScope());
        model.put(REDIRECT_URI, authorizeResponse.getRedirect());
        model.put(STATE, authorizeResponse.getState());
        // TODO: it's a poor idea to pass user credentials in hidden form fields
        model.put(USERNAME, authorizeResponse.getUsername());
        model.put(PASSWORD, authorizeResponse.getPassword());
        return new ModelAndView(CONSENT, model);
    }

    private void updateModelForLogin(final Model model, final String responseType, final String realmName,
            final String clientId, final Optional<String> scope, final Optional<String> state, final URI redirectUri,
            final Optional<String> error) {
        model.addAttribute(RESPONSE_TYPE, responseType);
        model.addAttribute(REALM, realmName);
        model.addAttribute(CLIENT_ID, clientId);
        model.addAttribute(SCOPE, scope.orElse(EMPTY));
        model.addAttribute(STATE, state.orElse(EMPTY));
        model.addAttribute(REDIRECT_URI, redirectUri.toString());
        model.addAttribute(ERROR, error.orElse(null));
    }

    private URI generateCodeResponseURI(final String realmName, final Optional<String> state, final String clientId,
            final Set<String> finalScopes, final Map<String, String> claims, final URI redirectUri)
        throws URISyntaxException {
        final String code = cassandraAuthorizationCodeService.create(state.orElse(EMPTY), clientId, realmName,
                finalScopes, claims, redirectUri);

        return new URIBuilder(redirectUri).addParameter(CODE, code).addParameter(STATE, state.orElse(EMPTY)).build();
    }

    private URI generateTokenResponseURI(final UserRealm userRealm, final Optional<String> state, final String clientId,
            final Set<String> finalScopes, final Map<String, String> claims, final URI redirectUri)
        throws URISyntaxException, JOSEException {
        final String rawJWT = jwtIssuer.issueAccessToken(userRealm, clientId, finalScopes, claims);
        return new URIBuilder(redirectUri).addParameter(ACCESS_TOKEN, rawJWT).addParameter(TOKEN_TYPE, BEARER)
                                          .addParameter(EXPIRES_IN,
                                              String.valueOf(JWTIssuer.EXPIRATION_TIME.getSeconds()))
                                          .addParameter(SCOPE, ScopeProperties.join(finalScopes))
                                          .addParameter(STATE, state.orElse(EMPTY)).build();
    }

    private URI generateLoginFormURI(final String realmName, final String responseType, final Optional<String> state,
            final String clientId, final Set<String> finalScopes, final URI redirectUri) throws URISyntaxException,
        JOSEException {
        return new URIBuilder(AUTHORIZE_ENDPOINT).addParameter(RESPONSE_TYPE_SNAKECASE, responseType)
                                                 .addParameter(REALM, realmName)
                                                 .addParameter(CLIENT_ID_SNAKECASE, clientId)
                                                 .addParameter(SCOPE, ScopeProperties.join(finalScopes))
                                                 .addParameter(REDIRECT_URI_SNAKECASE, redirectUri.toString())
                                                 .addParameter(STATE, state.orElse(EMPTY))
                                                 .addParameter(ERROR, ACCESS_DENIED_ERROR).build();
    }

    private AuthorizeResponse generateConsentNeededResponse(final ClientData clientData, final Set<String> finalScopes,
            final String realmName, final String clientId, final URI redirectUri, final String username,
            final String password, final String responseType, final Optional<String> state) {

        final AuthorizeResponse authorizeResponse = new AuthorizeResponse();
        authorizeResponse.setRedirect(redirectUri.toString());
        authorizeResponse.setClientName(clientData.getName());
        authorizeResponse.setClientDescription(clientData.getDescription());
        authorizeResponse.setScopes(finalScopes);
        authorizeResponse.setClientId(clientId);
        authorizeResponse.setResponseType(responseType);
        authorizeResponse.setState(state.orElse(EMPTY));
        authorizeResponse.setScope(ScopeProperties.join(finalScopes));
        authorizeResponse.setRealm(realmName);
        authorizeResponse.setConsentNeeded(true);
        authorizeResponse.setUsername(username);
        authorizeResponse.setPassword(password);

        return authorizeResponse;
    }

    private AuthorizeResponse noConsentedScopesResponse(final URI redirectUri, final Optional<String> state)
        throws URISyntaxException {
        final AuthorizeResponse authorizeResponse = new AuthorizeResponse();
        final URI redirect = new URIBuilder(redirectUri).addParameter(ERROR, ACCESS_DENIED_ERROR)
                                                        .addParameter(STATE, state.orElse(EMPTY)).build();
        authorizeResponse.setRedirect(redirect.toString());
        return authorizeResponse;
    }
}
