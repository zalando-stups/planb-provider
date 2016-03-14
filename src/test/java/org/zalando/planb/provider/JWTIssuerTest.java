package org.zalando.planb.provider;

import com.datastax.driver.core.Row;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

import com.datastax.driver.core.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import org.junit.Test;
import org.mockito.Mockito;

import java.text.ParseException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class JWTIssuerTest {

    private static final String TEST_RS256_PEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIEpAIBAAKCAQEA3dEMoTXmK7/+WJJpnyjR6hzcG4fKhLfQ0i5VOyT7wny2yw0P\n" +
            "oP+lxaYQ/M+khkECcPoCElCfu2n2iRzrLkM2VoCNybScqFoAiClgEXUqC8x8P7VR\n" +
            "1rKkR3/7egDbS5H+lVtBtiYczXHqapQvA8S7tp/SozDfod6mqHXhMilXvPGU+81K\n" +
            "hkpSrXte7kwXQ/LyGGSSaeOKwTnhDCkKcTpBT0zVV9IRw5WLmrqfiWAsc4Dwxe/+\n" +
            "PQBc44oSIy80v55Oc5Ym3TY1qWHPKxKKwYO0xby8z7yfJNLBUUu66nsNJQD8dglU\n" +
            "92+4u7LnTwzIN4gaUdGSEiL4aiOwBCLtwUmNfwIDAQABAoIBAQCizX1skSfHspnW\n" +
            "nleBZq9zGg1+81JzyIouyazqxQE3RNvQ+lwfOaONBo7vTuOdWfeVvhlpId2q62H0\n" +
            "TtJaWSB3qtDmW3ajpbNAPMXy6kCa+lGzXLydTO6AP/HuINTTiWsGaj03mI+JRnPc\n" +
            "F7jOFkYECxfJiOSKZOWDPUm0zF1GIdcne32UJxFLawh8/GHE+NxuHcBK/JJrBGEt\n" +
            "pzqHyVFHiUasY1dxZGpoggmUGpPpZ8WrK8S+CU/yYyFH1WdPeMxPajFvFyvSLxmF\n" +
            "VTQyqAKUvbA18vrAIipKOd4aHq9apXH6AcghVUbQzoVUgHBMBqJwyNeTvmCfyvfn\n" +
            "fC8Ii0lJAoGBAP9sfleXs2UH89gv+HgKgJ9vYELs1y3h6pKOUbqK/IQ1Yl33cGEK\n" +
            "1H08ZxQBOIBTigJ+VZ0oyQXFTFXT3tECwmD9iIKB75evBGo/Y8wCmFBSx/e2VyxE\n" +
            "RHO6958ZvqXe28zQ9dnjctgcEkiUDVeyP/htOy9QPo+EIWYQ0WQkCfvVAoGBAN5R\n" +
            "JdMR/Sy+RS+5+U0jpM0bUGTAdYwbwnn/9bNVLo9B1DVBjkOb1D5U+nJc28trSll6\n" +
            "evaOtygiy+b3iExAdtnLcxfmCWGPKVcJx946GWVJM9p66wZ4UoMP29DmrMfKBgQx\n" +
            "SKB7PuXRR/lNa+6sjd2n1XIhQoIqxIxju29EDDIDAoGAI3ZhuDGUs6M+BLbsTWZP\n" +
            "41LoT4JogbNLCRv/VuHzGzv6M0emb6K8S8L2IL3mpVJz59K4ekBuYIG7DnODDQvQ\n" +
            "Bv1MVapKpImlGEdCyNFXaleD8e17/uZfhp3fwVQmtwrRA4fDioPcrwp+s1ry20kh\n" +
            "PpGT1QbUGMLkjDIrkDa8uB0CgYBiJ4PCr/OOuKcFTl/SfgU6BXA2O37qkCsKAEdz\n" +
            "mQ1IdLEDnmD4WqmXp583tOXZ5xHZdakqiJI3Jz3NSMalm+SdfiTfjVHg//spkYjs\n" +
            "BczGmk5JjPGNVrxfXzYXAnGQeBK18Sh2qlN31jGn0VXw98Be98XWcPbTT5yduz66\n" +
            "/llADwKBgQDy2g9e+WlvgUHQK76gPVX0LYeyl04HjL6f2cFRV2KB27H4rbOzVpV3\n" +
            "PfAYw8kJfub0KSTHDfBsP+4kCCQCZ9Cq6tWdk3CkJyCVJ1tOQ1eCt8JxW+mAkIa1\n" +
            "ZJleYgmy4L+qZ9SWZixHBxVZba+XLXQ4sdba7FwQ0CumHTbzuElKfg==\n" +
            "-----END RSA PRIVATE KEY-----\n";

    private Row getStoredKey() {
        Row row = Mockito.mock(Row.class);
        when(row.getString("kid")).thenReturn("mykey");
        when(row.getSet("realms", String.class)).thenReturn(ImmutableSet.of("myrealm"));
        when(row.getString("private_key_pem")).thenReturn(TEST_RS256_PEM);
        when(row.getString("algorithm")).thenReturn("RS256");
        when(row.getInt("valid_from")).thenReturn(0);
        return row;
    }

    @Test
    public void testGetSignedJWT() throws JOSEException, ParseException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder().claim("foo", "/bar").build();
        OIDCKeyHolder keyHolder = new OIDCKeyHolder();

        List<Row> storedKeys = ImmutableList.of(getStoredKey());
        keyHolder = Mockito.spy(keyHolder);
        Mockito.doReturn(storedKeys).when(keyHolder).getStoredKeys();
        keyHolder.checkKeys();
        OIDCKeyHolder.Signer signer = keyHolder.getCurrentSigner("myrealm").get();
        final String rawJWT = JWTIssuer.getSignedJWT(claims, signer);
        assertThat(rawJWT).isNotEmpty();

        final String jsonPayload = new String(Base64.getDecoder().decode(rawJWT.split("\\.")[1]));
        assertThat(jsonPayload).contains("/bar");
        // make sure our serialized JSON does NOT contain a stupid unnecessary backslash escape for the forward slash..
        assertThat(jsonPayload).doesNotContain("\\/bar");

        // check JWT contents
        JWT jwt = JWTParser.parse(rawJWT);
        assertThat(jwt.getHeader().toJSONObject()).containsOnlyKeys("kid", "alg");
        assertThat(jwt.getJWTClaimsSet().getClaims()).containsOnlyKeys("foo");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("foo")).isEqualTo("/bar");
    }
}
