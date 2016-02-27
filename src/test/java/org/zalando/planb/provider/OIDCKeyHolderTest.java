package org.zalando.planb.provider;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by hjacobs on 27.02.16.
 */
public class OIDCKeyHolderTest {


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

    private Row getStoredKey(String kid, int validFrom) {
        Row row = Mockito.mock(Row.class);
        when(row.getString("kid")).thenReturn(kid);
        when(row.getSet("realms", String.class)).thenReturn(ImmutableSet.of("myrealm"));
        when(row.getString("private_key_pem")).thenReturn(TEST_RS256_PEM);
        when(row.getString("algorithm")).thenReturn("RS256");
        when(row.getInt("valid_from")).thenReturn(validFrom);
        return row;
    }

    @Test
    public void testNoKeys() {
        OIDCKeyHolder keyHolder = new OIDCKeyHolder();

        List<Row> storedKeys = Lists.emptyList();
        keyHolder = Mockito.spy(keyHolder);
        Mockito.doReturn(storedKeys).when(keyHolder).getStoredKeys();
        keyHolder.checkKeys();
        Optional<OIDCKeyHolder.Signer> signer = keyHolder.getCurrentSigner("myrealm");
        assertThat(signer).isEmpty();
    }

    @Test
    public void testUseYoungestKey() {
        OIDCKeyHolder keyHolder = new OIDCKeyHolder();

        int futureTimestamp = (int) (System.currentTimeMillis() / 1000) + 3600;
        List<Row> storedKeys = ImmutableList.of(
                getStoredKey("oldkey", 123),
                getStoredKey("newkey", 999),
                getStoredKey("otherkey", 500),
                getStoredKey("futurekey", futureTimestamp));
        keyHolder = Mockito.spy(keyHolder);
        Mockito.doReturn(storedKeys).when(keyHolder).getStoredKeys();
        keyHolder.checkKeys();
        Optional<OIDCKeyHolder.Signer> signer = keyHolder.getCurrentSigner("myrealm");
        assertThat(signer).isPresent();
        assertThat(signer.get().getKid()).isEqualTo("newkey");
    }

    @Test
    public void testNewKeyForExistingRealm() {
        OIDCKeyHolder keyHolder = new OIDCKeyHolder();

        List<Row> storedKeys = ImmutableList.of(getStoredKey("oldkey", 1));
        keyHolder = Mockito.spy(keyHolder);
        Mockito.doReturn(storedKeys).when(keyHolder).getStoredKeys();
        keyHolder.checkKeys();
        Optional<OIDCKeyHolder.Signer> signer = keyHolder.getCurrentSigner("myrealm");
        assertThat(signer).isPresent();
        assertThat(signer.get().getKid()).isEqualTo("oldkey");

        List<Row> newKeys = ImmutableList.of(getStoredKey("newkey", 99));
        Mockito.doReturn(newKeys).when(keyHolder).getStoredKeys();
        keyHolder.checkKeys();

        signer = keyHolder.getCurrentSigner("myrealm");
        assertThat(signer).isPresent();
        assertThat(signer.get().getKid()).isEqualTo("newkey");
    }
}
