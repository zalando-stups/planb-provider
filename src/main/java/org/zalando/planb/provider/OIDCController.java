package org.zalando.planb.provider;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@RestController
public class OIDCController {
    @Autowired
    private RealmConfig realms;

    @Autowired
    private OIDCKeyHolder keyHolder;

    /**
     * https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples
     */
    @RequestMapping(value = "/oauth2/access_token", method = RequestMethod.POST)
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "realm", required = true) String realmName,
                                        @RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,
                                        @RequestParam(value = "scope", required = false) String scope,
                                        @RequestHeader(name = "Authorization", required = false) String authorization)
            throws RealmAuthenticationException, JoseException, RealmAuthorizationException {

        // check for supported grant types
        if (!"password".equals(grantType)) {
            throw new UnsupportedOperationException("unsupported grant type");
        }

        // retrieve realms for the given realm
        ClientRealm clientRealm = realms.getClientRealm(realmName);
        if (clientRealm == null) {
            throw new UnsupportedOperationException("realm unknown (client)");
        }

        UserRealm userRealm = realms.getUserRealm(realmName);
        if (userRealm == null) {
            throw new UnsupportedOperationException("realm unknown (user)");
        }

        // parse requested scopes
        String[] scopes = scope.split(" ");

        // do the authentication
        System.out.println("DEBUG Authorization: " + authorization); // TODO remove
        clientRealm.authenticate("TODOclientId", "test", scopes); // TODO take clientId and clientSecret from basic auth
        Map<String, Object> extraClaims = userRealm.authenticate(username, password, scopes);

        // request authorized, create and return JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer("PlanB");

        long expiration = System.currentTimeMillis() + (8 * 60 * 60 * 1000);
        claims.setExpirationTime(NumericDate.fromMilliseconds(expiration));

        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setSubject(username); // can be overridden by the user realm

        claims.setStringListClaim("scope", scopes);
        claims.setStringClaim("realm", realmName);
        extraClaims.forEach(claims::setClaim);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(keyHolder.getJsonWebKey().getPrivateKey());
        jws.setKeyIdHeaderValue(keyHolder.getJsonWebKey().getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

        String jwt = jws.getCompactSerialization();

        return new OIDCCreateTokenResponse(jwt, jwt, (expiration / 1000), scope, realmName);
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
    @JsonSerialize(using = OIDCSigningKeysSerializer.class)
    OIDCSigningKeysResponse getSigningKeys() {
        return new OIDCSigningKeysResponse(new ArrayList<JsonWebKey>() {{
            add(keyHolder.getJsonWebKey());
        }});
    }
}
