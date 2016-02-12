package org.zalando.planb.provider;

import org.jose4j.jwk.JsonWebKey;

import java.util.List;


public class OIDCSigningKeysResponse {
    private List<JsonWebKey> keys;

    public OIDCSigningKeysResponse(List<JsonWebKey> keys) {
        this.keys = keys;
    }

    public List<JsonWebKey> getKeys() {
        return keys;
    }
}