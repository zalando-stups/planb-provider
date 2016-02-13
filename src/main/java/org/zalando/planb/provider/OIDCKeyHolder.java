package org.zalando.planb.provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Private keys must never leave this class!
 */
@Component
public class OIDCKeyHolder {
    private static final Logger LOG = LoggerFactory.getLogger(OIDCKeyHolder.class);

    private static final long DELAY_IN_MS = 1000 * 60 * 1; // minutes

    // those information are updated (swapped) during runtime in the background
    private final AtomicReference<List<JWK>> publicJwks = new AtomicReference<>();
    private final AtomicReference<JWSSigner> currentSigner = new AtomicReference<>();

    @PostConstruct
    void initializeKeys() throws Exception {
        // fail boot if we cannot bootstrap keys as we would not be able to sign any incoming request
        loadKeys();
    }

    @Scheduled(fixedDelay = DELAY_IN_MS, initialDelay = DELAY_IN_MS)
    void checkKeys() {
        // keep list updated in memory
        try {
            loadKeys();
        } catch (Exception e) {
            LOG.error("Could not update key list.", e);
        }
    }

    private void loadKeys() throws JOSEException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        List<JWK> privateJwks = new ArrayList<>();

        // fetch list from cassandra
        // TODO implement cassandra fetching instead of inmemory generation
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
        ECGenParameterSpec kpgparams = new ECGenParameterSpec("secp256r1");
        keyGenerator.initialize(kpgparams);
        KeyPair keyPair = keyGenerator.generateKeyPair();

        JWK testJwk = new ECKey.Builder(ECKey.Curve.P_256, (ECPublicKey) keyPair.getPublic())
                .privateKey((ECPrivateKey) keyPair.getPrivate())
                .keyID("testkey")
                .keyUse(KeyUse.SIGNATURE)
                .build();
        privateJwks.add(testJwk);

        // sort by validFrom
        // TODO implement

        // generate public key list from private key list to prevent accidental exposal of private key and make it
        // immutable
        List<JWK> publicJwks = Collections.unmodifiableList(
                privateJwks.stream()
                        .map(JWK::toPublicJWK)
                        .collect(Collectors.toList()));

        // take the youngest (already) valid key
        JWSSigner signer = new ECDSASigner((ECPrivateKey) keyPair.getPrivate());

        // swap the current information, forget about all other private keys
        this.publicJwks.set(publicJwks);
        this.currentSigner.set(signer);
    }

    JWSSigner getCurrentSigner() {
        return currentSigner.get();
    }

    public List<JWK> getPublicJwks() {
        return publicJwks.get();
    }
}
