package org.zalando.planb.provider;

import java.io.IOException;
import java.util.List;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.lang.JoseException;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Lists;

public class SerializableTest {

    @Test
    public void test() throws JsonGenerationException, JsonMappingException, IOException {
        OIDCSigningKeysResponse response = new OIDCSigningKeysResponse(Lists.newArrayList());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(System.out, response);
    }

    @Test
    public void testWithKeys() throws JsonGenerationException, JsonMappingException, IOException, JoseException {
        JsonWebKey key = new OIDCKeyHolder().getJsonWebKey();
        List<JsonWebKey> keys = Lists.newArrayList();
        keys.add(key);
        keys.add(key);
        OIDCSigningKeysResponse response = new OIDCSigningKeysResponse(keys);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.writeValue(System.out, response);
    }
}
