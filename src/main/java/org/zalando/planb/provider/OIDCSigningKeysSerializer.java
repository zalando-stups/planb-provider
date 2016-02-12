package org.zalando.planb.provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.jose4j.jwk.JsonWebKey;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OIDCSigningKeysSerializer extends StdSerializer<OIDCSigningKeysResponse> {
    public OIDCSigningKeysSerializer() {
        super(OIDCSigningKeysResponse.class);
    }

    @Override
    public void serialize(OIDCSigningKeysResponse response, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        Map<String, Object> data = new HashMap<>();
        List<Object> keys = response.getKeys().stream()
                .map(key -> key.toParams(JsonWebKey.OutputControlLevel.PUBLIC_ONLY))
                .collect(Collectors.toList());
        data.put("keys", keys);
        jsonGenerator.writeObject(data);
    }
}
