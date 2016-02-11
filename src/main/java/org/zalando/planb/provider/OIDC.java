package org.zalando.planb.provider;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;

@Controller
public class OIDC {

    @Autowired
    private Realm realm;

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
            throws Realm.AuthenticationFailedException, JoseException {

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
    @ResponseBody
    OIDCDiscoveryInformationResponse getDiscoveryInformation() {
        return new OIDCDiscoveryInformationResponse();
    }

    @RequestMapping("/oauth2/v3/certs")
    @ResponseBody
    OIDCSigningKeysResponse getSigningKeys() {
        return new OIDCSigningKeysResponse(new ArrayList<JsonWebKey>() {{
            add(keyHolder.getJsonWebKey());
        }});
    }
}
