package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
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

    @Autowired
    private RealmConfig realms;

    @Autowired
    private OIDCKeyHolder keyHolder;

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private ScopeProperties scopeProperties;

    /**
     * Get client_id and client_secret from HTTP Basic Auth
     */
    public static String[] getClientCredentials(Optional<String> authorization) throws RealmAuthenticationException {
        return authorization
                .filter(string -> string.toUpperCase().startsWith(BASIC_AUTH_PREFIX.toUpperCase()))
                .map(string -> string.substring(BASIC_AUTH_PREFIX.length()))
                .map(BASE_64_DECODER::decode)
                .map(bytes -> new String(bytes, UTF_8))
                .map(string -> string.split(":"))
                .filter(array -> array.length == 2)
                .orElseThrow(() -> new BadRequestException(
                        "Malformed or missing Authorization header.",
                        "invalid_client",
                        "Client authentication failed"));
    }

    /**
     * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
     */
    @RequestMapping(value = {"/oauth2/access_token", "/z/oauth2/access_token"}, method = RequestMethod.POST)
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "realm") Optional<String> realmNameParam,
                                        @RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,
                                        @RequestParam(value = "scope") Optional<String> scope,
                                        @RequestHeader(name = "Authorization") Optional<String> authorization,
                                        @RequestHeader(name = "Host") Optional<String> hostHeader)
            throws RealmAuthenticationException, RealmAuthorizationException, JOSEException {
        final Metric metric = new Metric(metricRegistry).start();

        final String realmName = realmNameParam.orElseGet(() -> realms.findRealmNameInHost(hostHeader
                .orElseThrow(() -> new BadRequestException("Missing realm parameter and no Host header.", "missing_realm", "Missing realm parameter and no Host header.")))
                .orElseThrow(() -> new RealmNotFoundException(hostHeader.get())));

        try {
            if (username.trim().isEmpty() || password.trim().isEmpty()) {
                throw new BadRequestException(
                        "Username and password should be provided.",
                        "invalid_grant",
                        "The provided access grant is invalid, expired, or revoked.");
            }

            // check for supported grant types
            if (!"password".equals(grantType)) {
                throw new BadRequestException(
                        "Grant type is not supported: " + grantType,
                        "unsupported_grant_type",
                        "Grant type is not supported: " + grantType);
            }

            // retrieve realms for the given realm
            ClientRealm clientRealm = realms.getClientRealm(realmName);
            if (clientRealm == null) {
                throw new RealmNotFoundException(realmName);
            }

            UserRealm userRealm = realms.getUserRealm(realmName);
            if (userRealm == null) {
                throw new RealmNotFoundException(realmName);
            }

            // parse requested scopes
            final Set<String> scopes = ScopeProperties.split(scope);
            final Set<String> defaultScopes = scopeProperties.getDefaultScopes(realmName);
            final Set<String> finalScopes = scopes.isEmpty() ? defaultScopes : scopes;

            // do the authentication
            final String[] clientCredentials = getClientCredentials(authorization);

            clientRealm.authenticate(clientCredentials[0], clientCredentials[1], scopes, defaultScopes);
            final Map<String, Object> extraClaims = userRealm.authenticate(username, password, scopes, defaultScopes);

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

        SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm, null, null, null,
                null, null, null, null, null, null, signer.getKid(), null, null), claims);
        jwt.sign(signer.getJWSSigner());

        // done
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
