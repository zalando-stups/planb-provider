package org.zalando.planb.provider;

import org.jose4j.jwk.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;

@Component
public class OIDCKeyHolder {
    private final PublicJsonWebKey jsonWebKey;

    public OIDCKeyHolder() throws JoseException {
        // TODO only for test mode
        jsonWebKey = EcJwkGenerator.generateJwk(EllipticCurves.P256);
        jsonWebKey.setKeyId("testkey");
        jsonWebKey.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        jsonWebKey.setUse("sig");
    }

    PublicJsonWebKey getJsonWebKey() {
        return jsonWebKey;
    }
}
