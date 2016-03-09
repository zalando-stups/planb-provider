package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;
import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONStyle;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;
import static org.zalando.planb.provider.ClientRealmAuthenticationException.clientNotFound;
import static org.zalando.planb.provider.Metric.trimSlash;
import static org.zalando.planb.provider.ScopeProperties.SPACE;

@RestController
public class OIDCController {
    private static final long EXPIRATION_TIME = 8;
    private static final TimeUnit EXPIRATION_TIME_UNIT = TimeUnit.HOURS;

    private static final Joiner COMMA_SEPARATED = Joiner.on(",");

    private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

    private static final String BASIC_AUTH_PREFIX = "Basic ";

    // we just need one char to identify ourselves as "Plan B Provider" (Base64 has 33% overhead)
    private static final String ISSUER = "B";

    private final Logger log = getLogger(getClass());

    @Autowired
    private RealmConfig realms;

    @Autowired
    private OIDCKeyHolder keyHolder;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private ScopeProperties scopeProperties;

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
        return new ClientCredentials(basicAuth[0], basicAuth[1]);
    }

    public static ClientCredentials getClientCredentials(Optional<String> authorization, Optional<String> clientId, Optional<String> clientSecret) throws RealmAuthenticationException {
        if (clientId.isPresent() && clientSecret.isPresent()) {
            return new ClientCredentials(clientId.get(), clientSecret.get());
        } else {
            return getClientCredentials(authorization);
        }
    }

    /*
     * Check the given Redirect URI against the ones configured for the given client.
     * Throw BAD REQUEST exception if it does not match.
     */
    void validateRedirectUri(String realm, String clientId, ClientData clientData, URI redirectUri) {
        if (!clientData.getRedirectUris().contains(redirectUri.toString())) {
            throw new BadRequestException(format("Redirect URI mismatch for client %s/%s", realm, clientId), "invalid_request", "Redirect URI mismatch");
        }
    }

    @RequestMapping(value = "/oauth2/authorize")
    String showAuthorizationForm(@RequestParam(value = "realm") Optional<String> realmNameParam,
                                 @RequestParam(value = "response_type", required = true) String responseType,
                                 @RequestParam(value = "client_id", required = true) String clientId,
                                 @RequestParam(value = "scope") Optional<String> scope,
                                 @RequestParam(value = "redirect_uri") Optional<URI> redirectUriParam,
                                 @RequestParam(name = "state") Optional<String> state,
                                 @RequestHeader(name = "Host") Optional<String> hostHeader,
                                 HttpServletResponse response) throws IOException {

        final String realmName = getRealmName(realmNameParam, hostHeader);

        // retrieve realms for the given realm
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

        // TODO: use a proper view template here :-)
        Escaper escaper = HtmlEscapers.htmlEscaper();
        return "<html>" +
                "<head><title>Login</title><style>body { font: 17px Arial, Helvetica, sans-serif; }</style></head>" +
                "</body>" +
                "<div style=\"max-width: 612px; margin: 0 auto\">" +
                "<h1>Sign in</h1>" +
                "<form action=\"/oauth2/authorize\" method=\"post\">" +
                "<input type=\"hidden\" name=\"realm\" value=\"" + escaper.escape(realmName) + "\"/>" +
                "<input type=\"hidden\" name=\"client_id\" value=\"" + escaper.escape(clientId) + "\"/>" +
                "<input type=\"hidden\" name=\"scope\" value=\"" + escaper.escape(scope.orElse("")) + "\"/>" +
                "<input type=\"hidden\" name=\"state\" value=\"" + escaper.escape(state.orElse("")) + "\"/>" +
                "<input type=\"hidden\" name=\"redirect_uri\" value=\"" + escaper.escape(redirectUri.toString()) + "\"/>" +
                "<div><label>User Name:</label><input name=\"username\" /></div>" +
                "<div><label>Password:</label><input name=\"password\" type=\"password\"/></div>" +
                "<button type=\"submit\">Log in</button></form>" +
                "</div>" +
                "</body></html>";
        //
    }

    @RequestMapping(value = "/oauth2/authorize", method = RequestMethod.POST)
    void authorize(@RequestParam(value = "realm") Optional<String> realmNameParam,
                   @RequestParam(value = "client_id", required = true) String clientId,
                   @RequestParam(value = "scope") Optional<String> scope,
                   @RequestParam(value = "redirect_uri") URI redirectUri,
                   @RequestParam(name = "state") Optional<String> state,
                   @RequestParam(value = "username", required = true) String username,
                   @RequestParam(value = "password", required = true) String password,
                   @RequestHeader(name = "Host") Optional<String> hostHeader,
                   HttpServletResponse response) throws IOException, URISyntaxException {

        final String realmName = getRealmName(realmNameParam, hostHeader);

        // retrieve realms for the given realm
        ClientRealm clientRealm = realms.getClientRealm(realmName);

        final ClientData clientData = clientRealm.get(clientId).orElseThrow(() -> clientNotFound(clientId, realmName));

        // make sure (again!) that the redirect_uri was configured in the client
        validateRedirectUri(realmName, clientId, clientData, redirectUri);

        final Set<String> scopes = ScopeProperties.split(scope);
        final Set<String> defaultScopes = scopeProperties.getDefaultScopes(realmName);
        final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;

        UserRealm userRealm = realms.getUserRealm(realmName);

        final Map<String, String> claims = userRealm.authenticate(username, password, finalScopes, defaultScopes);

        final String code = cassandraAuthorizationCodeService.create(state.orElse(""), clientId, realmName, finalScopes, claims, redirectUri);

        URI redirect = new URIBuilder(redirectUri).addParameter("code", code).addParameter("state", state.orElse("")).build();
        response.sendRedirect(redirect.toString());
    }

    String getRealmName(Optional<String> realmNameParam, Optional<String> hostHeader) {
        final String realmName = realmNameParam.orElseGet(() -> realms.findRealmNameInHost(hostHeader
                .orElseThrow(() -> new BadRequestException("Missing realm parameter and no Host header.", "missing_realm", "Missing realm parameter and no Host header.")))
                .orElseThrow(() -> new RealmNotFoundException(hostHeader.get())));
        return realmName;
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
            @RequestHeader(name = "Authorization") Optional<String> authorization) throws JOSEException {

        final AuthorizationCode authCode = cassandraAuthorizationCodeService.invalidate(code)
                .orElseThrow(() -> new BadRequestException("Invalid authorization code", "invalid_request", "Invalid authorization code"));

        // TODO: check redirect_uri

        final String realmName = authCode.getRealm();

        // retrieve realms for the given realm
        ClientRealm clientRealm = realms.getClientRealm(realmName);
        UserRealm userRealm = realms.getUserRealm(realmName);

        final ClientCredentials clientCredentials = getClientCredentials(authorization, clientIdParam, clientSecretParam);
        clientRealm.authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), authCode.getScopes(), authCode.getScopes());

        if (!clientCredentials.getClientId().equals(authCode.getClientId())) {
            // authorization code can only be used by the client who requested it
            throw new BadRequestException("Invalid authorization code: client mismatch", "invalid_request", "Invalid authorization code: client mismatch");
        }

        final Map<String, String> extraClaims = authCode.getClaims();

        // this should never happen (only if some realm does not return "sub"
        Preconditions.checkState(extraClaims.containsKey(Realm.SUB), "'sub' claim missing");

        // request authorized, create JWT
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .expirationTime(new Date(System.currentTimeMillis() + EXPIRATION_TIME_UNIT.toMillis(EXPIRATION_TIME)))
                .issueTime(new Date())
                .claim("realm", realmName)
                .claim("scope", authCode.getScopes());
        extraClaims.forEach(claimsBuilder::claim);
        final JWTClaimsSet claims = claimsBuilder.build();

        // sign JWT
        OIDCKeyHolder.Signer signer = keyHolder.getCurrentSigner(realmName)
                .orElseThrow(() -> new UnsupportedOperationException("No key found for signing requests of realm " + realmName));

        final Metric signingMetric = new Metric(metricRegistry).start();
        String rawJWT;
        try {
            rawJWT = getSignedJWT(claims, signer);
        } finally {
            signingMetric.finish("planb.provider.jwt.signing." + signer.getAlgorithm().getName());
        }

        final String maskedSubject = userRealm.maskSubject((String) extraClaims.get(Realm.SUB));
        log.info("Issued JWT for '{}' requested by client {}/{}", maskedSubject, realmName, clientCredentials.getClientId());

        return new OIDCCreateTokenResponse(
                rawJWT,
                rawJWT,
                EXPIRATION_TIME_UNIT.toSeconds(EXPIRATION_TIME),
                authCode.getScopes().stream().collect(joining(SPACE)),
                realmName);
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

        final String realmName = getRealmName(realmNameParam, hostHeader);

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

            // parse requested scopes
            final Set<String> scopes = ScopeProperties.split(scope);
            final Set<String> defaultScopes = scopeProperties.getDefaultScopes(realmName);
            final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;

            final ClientCredentials clientCredentials = getClientCredentials(authorization, clientIdParam, clientSecretParam);
            clientRealm.authenticate(clientCredentials.getClientId(), clientCredentials.getClientSecret(), scopes, defaultScopes);
            final Map<String, String> extraClaims = userRealm.authenticate(username, password, scopes, defaultScopes);

            // this should never happen (only if some realm does not return "sub"
            Preconditions.checkState(extraClaims.containsKey(Realm.SUB), "'sub' claim missing");

            // request authorized, create JWT
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .expirationTime(new Date(System.currentTimeMillis() + EXPIRATION_TIME_UNIT.toMillis(EXPIRATION_TIME)))
                    .issueTime(new Date())
                    .claim("realm", realmName)
                    .claim("scope", finalScopes);
            extraClaims.forEach(claimsBuilder::claim);
            final JWTClaimsSet claims = claimsBuilder.build();

            // sign JWT
            OIDCKeyHolder.Signer signer = keyHolder.getCurrentSigner(realmName)
                    .orElseThrow(() -> new UnsupportedOperationException("No key found for signing requests of realm " + realmName));

            final Metric signingMetric = new Metric(metricRegistry).start();
            String rawJWT;
            try {
                rawJWT = getSignedJWT(claims, signer);
            } finally {
                signingMetric.finish("planb.provider.jwt.signing." + signer.getAlgorithm().getName());
            }

            final String maskedSubject = userRealm.maskSubject((String) extraClaims.get(Realm.SUB));
            log.info("Issued JWT for '{}' requested by client {}/{}", maskedSubject, realmName, clientCredentials.getClientId());
            metric.finish("planb.provider.access_token." + trimSlash(realmName) + ".success");

            return new OIDCCreateTokenResponse(
                    rawJWT,
                    rawJWT,
                    EXPIRATION_TIME_UNIT.toSeconds(EXPIRATION_TIME),
                    finalScopes.stream().collect(joining(SPACE)),
                    realmName);
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

    static String getSignedJWT(JWTClaimsSet claims, OIDCKeyHolder.Signer signer) throws JOSEException {
        final JWSAlgorithm algorithm = signer.getAlgorithm();

        // NOTE: we are doing the JSON serialization "by hand" here to use the correct compression flag
        // (the default is using net.minidev.json.JStylerObj.ESCAPE4Web which also escapes forward slashes)
        final String serializedJson = claims.toJSONObject().toJSONString(JSONStyle.LT_COMPRESS);
        final JWSHeader header = new JWSHeader(algorithm, null, null, null, null, null, null, null, null, null,
                signer.getKid(), null, null);
        final Payload payload = new Payload(serializedJson);
        final JWSObject jwt = new JWSObject(header, payload);

        jwt.sign(signer.getJWSSigner());

        return jwt.serialize();
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
