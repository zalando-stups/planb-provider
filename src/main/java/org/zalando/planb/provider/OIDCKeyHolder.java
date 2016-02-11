package org.zalando.planb.provider;

import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;

@Component
public class OIDCKeyHolder {
    private final PublicJsonWebKey jsonWebKey;

    public OIDCKeyHolder() throws JoseException {
        // TODO only for test mode
        jsonWebKey = RsaJwkGenerator.generateJwk(2048);
        jsonWebKey.setKeyId("testkey");
        jsonWebKey.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
        jsonWebKey.setUse("sign");
    }

    PublicJsonWebKey getJsonWebKey() {
        return jsonWebKey;
    }
}
