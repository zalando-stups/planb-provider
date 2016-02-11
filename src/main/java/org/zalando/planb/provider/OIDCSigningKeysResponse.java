package org.zalando.planb.provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jose4j.jwk.JsonWebKey;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;


public class OIDCSigningKeysResponse {
    private List<JsonWebKey> keys;

    public OIDCSigningKeysResponse(List<JsonWebKey> keys) {
        this.keys = keys;
    }

    public List<JsonWebKey> getKeys() {
        return keys;
    }
}