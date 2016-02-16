package org.zalando.planb.provider;

import com.google.common.base.Joiner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
public class OIDCController {
    private static final long EXPIRATION_TIME = 8;
    private static final TimeUnit EXPIRATION_TIME_UNIT = TimeUnit.HOURS;

    private static final Joiner COMMA_SEPARATED = Joiner.on(",");

    private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

    private static final String BASIC_AUTH_PREFIX = "Basic ";

    @Autowired
    private RealmConfig realms;

    @Autowired
    private OIDCKeyHolder keyHolder;

    /**
     * Get client_id and client_secret from HTTP Basic Auth
     */
    public static String[] getClientCredentials(Optional<String> authorization) throws RealmAuthenticationException {
        final String[] clientCredentials = authorization
                .filter(string -> string.toUpperCase().startsWith(BASIC_AUTH_PREFIX.toUpperCase()))
                .map(string -> string.substring(BASIC_AUTH_PREFIX.length()))
                .map(BASE_64_DECODER::decode)
                .map(bytes -> new String(bytes, UTF_8))
                .map(string -> string.split(":"))
                .filter(array -> array.length == 2)
                .orElseThrow(() -> new InvalidInputException("Malformed or missing Authorization header."));
        return clientCredentials;
    }

    /**
     * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
     */
    @RequestMapping(value = {"/oauth2/access_token", "/z/oauth2/access_token"}, method = RequestMethod.POST)
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "realm", required = true) String realmName,
                                        @RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,
                                        @RequestParam(value = "scope") Optional<String> scope,
                                        @RequestHeader(name = "Authorization") Optional<String> authorization)
            throws RealmAuthenticationException, RealmAuthorizationException, JOSEException {

        // check for supported grant types
        if (!"password".equals(grantType)) {
            throw new InvalidInputException("Unsupported grant type: " + grantType);
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
        String[] scopes = scope.map(string -> string.split(" ")).orElse(new String[]{});

        // do the authentication
        final String[] clientCredentials = getClientCredentials(authorization);

        clientRealm.authenticate(clientCredentials[0], clientCredentials[1], scopes);
        Map<String, Object> extraClaims = userRealm.authenticate(username, password, scopes);

        // request authorized, create JWT
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer("PlanB")
                .expirationTime(new Date(System.currentTimeMillis() + EXPIRATION_TIME_UNIT.toMillis(EXPIRATION_TIME)))
                .issueTime(new Date())
                .subject(username)
                .claim("realm", realmName)
                .claim("scope", scopes);
        extraClaims.forEach(claimsBuilder::claim);
        JWTClaimsSet claims = claimsBuilder.build();

        // sign JWT
        Optional<OIDCKeyHolder.Signer> signer = keyHolder.getCurrentSigner(realmName);
        if (signer.isPresent()) {
            SignedJWT jwt = new SignedJWT(new JWSHeader(signer.get().getAlgorithm(), JOSEObjectType.JWT, null, null,
                    null, null, null, null, null, null, signer.get().getKid(), null, null), claims);
            jwt.sign(signer.get().getJWSSigner());

            // done
            String rawJWT = jwt.serialize();
            return new OIDCCreateTokenResponse(rawJWT, rawJWT, EXPIRATION_TIME_UNIT.toSeconds(EXPIRATION_TIME),
                    scope.orElse(""), realmName);
        } else {
            throw new UnsupportedOperationException("No key found for signing requests of realm " + realmName);
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

    @RequestMapping("/oauth2/v3/certs")
    String getSigningKeys() {
        List<String> jwks = keyHolder.getCurrentPublicKeys().stream()
                .map(JWK::toJSONString)
                .collect(Collectors.toList());

        return "{\"keys\": [" + COMMA_SEPARATED.join(jwks) + "]}";
    }
}
