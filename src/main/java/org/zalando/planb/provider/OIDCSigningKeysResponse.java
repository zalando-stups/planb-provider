package org.zalando.planb.provider;

import java.io.IOException;
import java.util.List;

import org.jose4j.jwk.JsonWebKey;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

//@JsonSerialize
public class OIDCSigningKeysResponse {


    private List<JsonWebKey> keys;

    protected OIDCSigningKeysResponse() {
        // jackson
    }

    public OIDCSigningKeysResponse(List<JsonWebKey> keys) {
        this.keys = keys;
    }

    // @JsonSerialize(using = JWKJsonSerializer.class)
    public List<JsonWebKey> getKeys() {
        return keys;
    }

    /*
    public static class Key {
        private String kty;
        private String alg;
        private String use;
        private String kid;
        private String n;
        private String e;

        public Key(PublicJsonWebKey key) {
            kty = key.getKeyType();
            alg = key.getAlgorithm();
            use = key.getUse();
            kid = key.getKeyId();
            n = new String(key.getPublicKey().getEncoded());
            e = "";
        }

        public String getKty() {
            return kty;
        }

        public String getAlg() {
            return alg;
        }

        public String getUse() {
            return use;
        }

        public String getKid() {
            return kid;
        }

        public String getN() {
            return n;
        }

        public String getE() {
            return e;
        }
    }
    */


    public class JWKJsonSerializer extends StdSerializer<JsonWebKey> {
        public JWKJsonSerializer() {
            super(JsonWebKey.class);
        }

        @Override
        public void serialize(JsonWebKey jsonWebKey, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonGenerationException {
            jsonGenerator.writeObject(jsonWebKey.toParams(JsonWebKey.OutputControlLevel.PUBLIC_ONLY));
        }
    }
}