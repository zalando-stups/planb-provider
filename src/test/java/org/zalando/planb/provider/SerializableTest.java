package org.zalando.planb.provider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SerializableTest {

    @Test
    public void test() throws JsonGenerationException, JsonMappingException, IOException {
        OIDCSigningKeysResponse response = new OIDCSigningKeysResponse(Lists.newArrayList());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(System.out, response);
    }

    @Ignore
    @Test
    public void testWithKeys() throws JsonGenerationException, JsonMappingException, IOException, JoseException {
        Map<String, Object> params = Maps.newHashMap();
        params.put("kty", "RSA");
        params.put("n", "what");
        params.put("e", "e");
        List<JsonWebKey> keys = Lists.newArrayList();
        keys.add(JsonWebKey.Factory.newJwk(params));
        keys.add(JsonWebKey.Factory.newJwk(params));
        OIDCSigningKeysResponse response = new OIDCSigningKeysResponse(keys);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(System.out, response);
    }
}
