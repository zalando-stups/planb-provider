package org.zalando.planb.provider;

import java.util.List;

import org.jose4j.jwk.JsonWebKey;


public class OIDCSigningKeysResponse {
    private List<JsonWebKey> keys;

    public OIDCSigningKeysResponse(List<JsonWebKey> keys) {
        this.keys = keys;
    }

    public List<JsonWebKey> getKeys() {
        return keys;
    }
}