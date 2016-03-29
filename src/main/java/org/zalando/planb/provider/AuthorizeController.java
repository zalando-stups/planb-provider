package org.zalando.planb.provider;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.zalando.planb.provider.OIDCController.getRealmName;
import static org.zalando.planb.provider.OIDCController.validateRedirectUri;
import static org.zalando.planb.provider.realms.ClientRealmAuthenticationException.clientNotFound;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.*;
import java.util.function.Supplier;

import com.google.common.collect.Collections2;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Controller
@RequestMapping(path = "/oauth2/authorize")
public class AuthorizeController {

    private static final String EMPTY_STRING = "";
    private static final String HEADER_HOST = "Host";
    private static final String LOGIN_FORM = "login";

    private static final String MODEL_CLIENT_DESCRIPTION = "clientDescription";
    private static final String MODEL_CLIENT_ID = "clientId";
    private static final String MODEL_CLIENT_NAME = "clientName";
    private static final String MODEL_CONSENT = "consent";
    private static final String MODEL_ERROR = "error";
    private static final String MODEL_PASSWORD = "password";
    private static final String MODEL_REALM = "realm";
    private static final String MODEL_REDIRECT_URI = "redirectUri";
    private static final String MODEL_RESPONSE_TYPE = "responseType";
    private static final String MODEL_SCOPE = "scope";
    private static final String MODEL_SCOPES = "scopes";
    private static final String MODEL_STATE = "state";
    private static final String MODEL_USERNAME = "username";

    private static final String PARAM_ACCESS_TOKEN = "access_token";
    private static final String PARAM_CLIENT_ID = "client_id";
    private static final String PARAM_DECISION = "decision";
    private static final String PARAM_DECISION_ALLOW = "allow";
    private static final String PARAM_DECISION_DENY = "deny";
    private static final String PARAM_DECISION_DEFAULT = "none";
    private static final String PARAM_ERROR = "error";
    private static final String PARAM_ERROR_ACCESS_DENIED = "access_denied";
    private static final String PARAM_EXPIRES_IN = "expires_in";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_REALM = "realm";
    private static final String PARAM_REDIRECT_URI = "redirect_uri";
    private static final String PARAM_RESPONSE_TYPE = "response_type";
    private static final String PARAM_RESPONSE_TYPE_CODE = "code";
    private static final String PARAM_SCOPE = "scope";
    private static final String PARAM_RESPONSE_TYPE_TOKEN = "token";
    private static final String PARAM_STATE = "state";
    private static final String PARAM_TOKEN_TYPE = "token_type";
    private static final String PARAM_TOKEN_TYPE_BEARER = "Bearer";
    private static final String PARAM_USERNAME = "username";

    static final Set<String> SUPPORTED_RESPONSE_TYPES = ImmutableSet.of(PARAM_RESPONSE_TYPE_CODE, PARAM_RESPONSE_TYPE_TOKEN);

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
    String showAuthorizationForm(@RequestParam(value = PARAM_REALM) final Optional<String> realmNameParam,
                                 @RequestParam(value = PARAM_RESPONSE_TYPE) final String responseType,
                                 @RequestParam(value = PARAM_CLIENT_ID) final String clientId,
                                 @RequestParam(value = PARAM_SCOPE) final Optional<String> scope,
                                 @RequestParam(value = PARAM_REDIRECT_URI) final Optional<URI> redirectUriParam,
                                 @RequestParam(value = PARAM_STATE) final Optional<String> state,
                                 @RequestParam(value = PARAM_ERROR) final Optional<String> error,
                                 @RequestHeader(value = HEADER_HOST) final Optional<String> hostHeader, final Model model) {

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
            @RequestParam(value = PARAM_RESPONSE_TYPE) final String responseType,
            @RequestParam(value = PARAM_REALM) final Optional<String> realmNameParam,
            @RequestParam(value = PARAM_CLIENT_ID) final String clientId,
            @RequestParam(value = PARAM_SCOPE) final Optional<String> scope,
            @RequestParam(value = PARAM_REDIRECT_URI) final URI redirectUri,
            @RequestParam(value = PARAM_STATE) final Optional<String> state,
            @RequestParam(value = PARAM_USERNAME) final String username,
            @RequestParam(value = PARAM_PASSWORD) final String password,
            @RequestParam(value = PARAM_DECISION) final Optional<String> decision,
            @RequestHeader(value = HEADER_HOST) final Optional<String> hostHeader) throws IOException, URISyntaxException,
        JOSEException {

        if (!SUPPORTED_RESPONSE_TYPES.contains(responseType)) {
            throw new BadRequestException("Unsupported response_type", "unsupported_response_type",
                "Unsupported response_type");
        }

        final String realmName = getRealmName(realms, realmNameParam, hostHeader);

        // retrieve realms for the given realm
        final ClientRealm clientRealm = realms.getClientRealm(realmName);
        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        // make sure (again!) that the redirect_uri was configured in the client
        validateRedirectUri(realmName, clientId, clientData, redirectUri);
        checkNonConfidentialClientForImplicitCodeGrant(clientId, responseType, clientData.isConfidential());

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
            else {
                redirect = generateRedirectURIbasedOnGrantType(responseType, realmName, userRealm, state, clientId, finalScopes, claims, redirectUri);
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
            @RequestParam(value = PARAM_RESPONSE_TYPE) final String responseType,
            @RequestParam(value = PARAM_REALM) final Optional<String> realmNameParam,
            @RequestParam(value = PARAM_CLIENT_ID) final String clientId,
            @RequestParam(value = PARAM_SCOPE) final Optional<String> scope,
            @RequestParam(value = PARAM_REDIRECT_URI) final URI redirectUri,
            @RequestParam(value = PARAM_STATE) final Optional<String> state,
            @RequestParam(value = PARAM_USERNAME) final String username,
            @RequestParam(value = PARAM_PASSWORD) final String password,
            @RequestParam(value = PARAM_DECISION) final Optional<String> decision,
            @RequestHeader(value = HEADER_HOST) final Optional<String> hostHeader) throws IOException, URISyntaxException,
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
        return consentedScopes.isPresent() && consentedScopes.get().containsAll(finalScopes);
    }

    private void checkNonConfidentialClientForImplicitCodeGrant(final String clientId, final String responseType, final boolean isConfidential) {

        if (isImplicitGrant(responseType) && isConfidential){
            throw new BadRequestException(format("Invalid response_type 'token' for confidential client %s", clientId),
                    "invalid_request", "Invalid response_type 'token' for confidential client");
        }

    }

    private boolean isAuthorizationCodeGrant(final String responseType) {
        return PARAM_RESPONSE_TYPE_CODE.equals(responseType);
    }

    private boolean isImplicitGrant(final String responseType) {
        return PARAM_RESPONSE_TYPE_TOKEN.equals(responseType);
    }

    private Optional<Set<String>> storeOrGetConsentedScopes(final Optional<String> decision, final String username,
            final String clientId, final UserRealm userRealm, final Set<String> finalScopes) throws URISyntaxException {
        Optional<Set<String>> consentedScopes = Optional.empty();

        switch (decision.orElse(PARAM_DECISION_DEFAULT)) {

            case PARAM_DECISION_ALLOW:
                // save user consent
                cassandraConsentService.store(username, userRealm.getName(), clientId, finalScopes);
                consentedScopes = Optional.of(finalScopes);
                break;

            case PARAM_DECISION_DENY:
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
        model.put(MODEL_CLIENT_NAME, authorizeResponse.getClientName());
        model.put(MODEL_CLIENT_DESCRIPTION, authorizeResponse.getClientDescription());
        model.put(MODEL_SCOPES, authorizeResponse.getScopes());
        model.put(MODEL_RESPONSE_TYPE, authorizeResponse.getResponseType());
        model.put(MODEL_REALM, authorizeResponse.getRealm());
        model.put(MODEL_CLIENT_ID, authorizeResponse.getClientId());
        model.put(MODEL_SCOPE, authorizeResponse.getScope());
        model.put(MODEL_REDIRECT_URI, authorizeResponse.getRedirect());
        model.put(MODEL_STATE, authorizeResponse.getState());
        // TODO: it's a poor idea to pass user credentials in hidden form fields
        model.put(MODEL_USERNAME, authorizeResponse.getUsername());
        model.put(MODEL_PASSWORD, authorizeResponse.getPassword());
        return new ModelAndView(MODEL_CONSENT, model);
    }

    private void updateModelForLogin(final Model model, final String responseType, final String realmName,
            final String clientId, final Optional<String> scope, final Optional<String> state, final URI redirectUri,
            final Optional<String> error) {
        model.addAttribute(MODEL_RESPONSE_TYPE, responseType);
        model.addAttribute(MODEL_REALM, realmName);
        model.addAttribute(MODEL_CLIENT_ID, clientId);
        model.addAttribute(MODEL_SCOPE, scope.orElse(EMPTY_STRING));
        model.addAttribute(MODEL_STATE, state.orElse(EMPTY_STRING));
        model.addAttribute(MODEL_REDIRECT_URI, redirectUri.toString());
        model.addAttribute(MODEL_ERROR, error.orElse(null));
    }

    private URI generateRedirectURIbasedOnGrantType(final String responseType, final String realmName,
                                                    final UserRealm userRealm, final Optional<String> state,
                                                    final String clientId, final Set<String> finalScopes,
                                                    final Map<String, String> claims, final URI redirectUri)
            throws URISyntaxException, JOSEException {

        if (isAuthorizationCodeGrant(responseType)) {
            return generateCodeResponseURI(realmName, state, clientId, finalScopes, claims, redirectUri);
        } else { // implicit grant
            // see http://tools.ietf.org/html/rfc7231#section-5.3.2
            return generateTokenResponseURI(userRealm, state, clientId, finalScopes, claims, redirectUri);
        }
    }

    private URI generateCodeResponseURI(final String realmName, final Optional<String> state, final String clientId,
            final Set<String> finalScopes, final Map<String, String> claims, final URI redirectUri)
        throws URISyntaxException {
        final String code = cassandraAuthorizationCodeService.create(state.orElse(EMPTY_STRING), clientId, realmName,
                finalScopes, claims, redirectUri);

        return new URIBuilder(redirectUri).addParameter(PARAM_RESPONSE_TYPE_CODE, code).addParameter(PARAM_STATE, state.orElse(EMPTY_STRING)).build();
    }

    private URI generateTokenResponseURI(final UserRealm userRealm, final Optional<String> state, final String clientId,
            final Set<String> finalScopes, final Map<String, String> claims, final URI redirectUri)
        throws URISyntaxException, JOSEException {

        final String rawJWT = jwtIssuer.issueAccessToken(userRealm, clientId, finalScopes, claims);
        return new URIBuilder(redirectUri).addParameter(PARAM_ACCESS_TOKEN, rawJWT).addParameter(PARAM_TOKEN_TYPE, PARAM_TOKEN_TYPE_BEARER)
                                          .addParameter(PARAM_EXPIRES_IN,
                                              String.valueOf(JWTIssuer.EXPIRATION_TIME.getSeconds()))
                                          .addParameter(PARAM_SCOPE, ScopeProperties.join(finalScopes))
                                          .addParameter(PARAM_STATE, state.orElse(EMPTY_STRING)).build();
    }

    private URI generateLoginFormURI(final String realmName, final String responseType, final Optional<String> state,
            final String clientId, final Set<String> finalScopes, final URI redirectUri) throws URISyntaxException,
        JOSEException {
        return new URIBuilder(linkTo(AuthorizeController.class).toUri().getPath())
                                                    .addParameter(PARAM_RESPONSE_TYPE, responseType)
                                                    .addParameter(PARAM_REALM, realmName)
                                                    .addParameter(PARAM_CLIENT_ID, clientId)
                                                    .addParameter(PARAM_SCOPE, ScopeProperties.join(finalScopes))
                                                    .addParameter(PARAM_REDIRECT_URI, redirectUri.toString())
                                                    .addParameter(PARAM_STATE, state.orElse(EMPTY_STRING))
                                                    .addParameter(PARAM_ERROR, PARAM_ERROR_ACCESS_DENIED).build();
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
        authorizeResponse.setState(state.orElse(EMPTY_STRING));
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
        final URI redirect = new URIBuilder(redirectUri).addParameter(PARAM_ERROR, PARAM_ERROR_ACCESS_DENIED)
                                                        .addParameter(PARAM_STATE, state.orElse(EMPTY_STRING)).build();
        authorizeResponse.setRedirect(redirect.toString());
        return authorizeResponse;
    }


}
