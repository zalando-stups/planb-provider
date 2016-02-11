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
import org.zalando.planb.provider.exception.AuthenticationFailedException;

import java.util.ArrayList;
import java.util.Map;

@RestController
public class OIDC {

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
                                        @RequestParam(value = "scope", required = false) String scope)
            throws AuthenticationFailedException, JoseException {

        Realm realm = realms.get(realmName); // TODO check availability
        if (realm == null) {
            throw new UnsupportedOperationException("realm unknown");
        }

        String[] scopes = scope.split(" ");
        Map<String, Object> extraClaims = realm.authenticate(username, password, scopes);

        JwtClaims claims = new JwtClaims();
        claims.setIssuer("PlanB");

        long expiration = System.currentTimeMillis() + (8 * 60 * 60 * 1000);
        claims.setExpirationTime(NumericDate.fromMilliseconds(expiration));

        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setSubject(username);

        claims.setStringListClaim("scopes", scopes);
        extraClaims.forEach(claims::setClaim);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(keyHolder.getJsonWebKey().getPrivateKey());
        jws.setKeyIdHeaderValue(keyHolder.getJsonWebKey().getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        String jwt = jws.getCompactSerialization();

        return new OIDCCreateTokenResponse(jwt, jwt, (expiration / 1000), scope);
    }

    @RequestMapping("/.well-known/openid-configuration")
    OIDCDiscoveryInformationResponse getDiscoveryInformation() {
        return new OIDCDiscoveryInformationResponse();
    }

    @RequestMapping("/oauth2/v3/certs")
    @JsonSerialize(using = OIDCSigningKeysSerializer.class)
    OIDCSigningKeysResponse getSigningKeys() {
        return new OIDCSigningKeysResponse(new ArrayList<JsonWebKey>() {{
            add(keyHolder.getJsonWebKey());
        }});
    }
}
