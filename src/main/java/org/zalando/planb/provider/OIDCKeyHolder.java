package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * Private keys must never leave this class!
 */
@Component
public class OIDCKeyHolder {
    private static final Logger LOG = LoggerFactory.getLogger(OIDCKeyHolder.class);

    private static final long DELAY_IN_MS = 1000 * 60 * 1; // minutes
    private static final String KEYPAIR_TABLE = "keypair";

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private Session session;

    // set on post construct
    private PreparedStatement fetchKeys;

    // those information are updated (swapped) during runtime in the background
    private final AtomicReference<List<JWK>> currentPublicKeys = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<Map<String, Signer>> currentSigner = new AtomicReference<>(new HashMap<>());

    Optional<Signer> getCurrentSigner(final String realm) {
        return Optional.ofNullable(currentSigner.get().get(realm));
    }

    public List<JWK> getCurrentPublicKeys() {
        return currentPublicKeys.get();
    }

    @PostConstruct
    void initializeKeys() throws Exception {
        prepareStatements();
        loadKeys();
        LOG.info("OIDC key holder initialized.");
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

    void prepareStatements() {
        fetchKeys = session.prepare(
                select().from(cassandraProperties.getKeyspace(), KEYPAIR_TABLE));
    }

    private void loadKeys() throws JOSEException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, IOException {
        final List<JWK> currentPublicKeys = this.currentPublicKeys.get();

        // fetch list from cassandra
        final List<Row> storedKeys = session.execute(fetchKeys.bind()).all();

        // figure out if we actually have changes (different set of keys)
        final Set<String> currentPublicKeyNames = currentPublicKeys.stream()
                .map(JWK::getKeyID)
                .collect(Collectors.toSet());
        final Set<String> storedKeyNames = storedKeys.stream()
                .map(row -> row.getString("kid"))
                .collect(Collectors.toSet());

        if (currentPublicKeyNames.containsAll(storedKeyNames) && storedKeyNames.containsAll(currentPublicKeyNames)) {
            // in memory list of JWK IDs matches database, nothing to do
            // TODO also consider row changes (check last modified?)
            return;
        }

        // transform results into proper JWKs
        // http://connect2id.com/products/nimbus-jose-jwt/openssl-key-generation
        final List<JWK> newPublicKeys = new ArrayList<>();
        final Multimap<String, Key> realmKeys = ArrayListMultimap.create();

        // skip the whole run if an error occurs, do not allow inconsistent key list
        for (final Row storedKey : storedKeys) {
            // retrieve row values
            final String kid = storedKey.getString("kid");
            final Set<String> realms = storedKey.getSet("realms", String.class);
            final String rawPrivateKeyPem = storedKey.getString("private_key_pem");
            final String rawAlgorithm = storedKey.getString("algorithm");
            final int rawValidFrom = storedKey.getInt("valid_from");

            final JWSAlgorithm algorithm = JWSAlgorithm.parse(rawAlgorithm);

            // parse key pair
            final PEMParser pemParser = new PEMParser(new StringReader(rawPrivateKeyPem));

            Object pemObject = pemParser.readObject();
            if (pemObject instanceof ASN1ObjectIdentifier) {
                /**
                 * Skip EC parameter header, could be retrieved with following code:
                 * ASN1ObjectIdentifier ecOID = (ASN1ObjectIdentifier) pemObject;
                 * X9ECParameters ecSpec = ECNamedCurveTable.getByOID(ecOID);
                 */
                // next entry is supposed to be the real key pair
                pemObject = pemParser.readObject();
            }
            pemParser.close();

            final PEMKeyPair pemKeyPair = (PEMKeyPair) pemObject;
            final KeyPair keyPair = new JcaPEMKeyConverter()
                    .getKeyPair(pemKeyPair);

            // create JWK from keypair
            final JWK jwk;
            switch (rawAlgorithm) {
                case "RS256":
                case "RS384":
                case "RS512":
                case "PS256":
                case "PS384":
                case "PS512":
                    jwk = createRSAJWK(kid, keyPair.getPublic(), algorithm);
                    break;
                case "ES256":
                    jwk = createECJWK(kid, keyPair.getPublic(), ECKey.Curve.P_256, algorithm);
                    break;
                case "ES384":
                    jwk = createECJWK(kid, keyPair.getPublic(), ECKey.Curve.P_384, algorithm);
                    break;
                case "ES512":
                    jwk = createECJWK(kid, keyPair.getPublic(), ECKey.Curve.P_521, algorithm);
                    break;
                default:
                    throw new UnsupportedOperationException("Algorithm not supported for key " + kid + ": " + rawAlgorithm);
            }

            newPublicKeys.add(jwk);

            // enlist key on all realms that it could be used
            for (String realm : realms) {
                realmKeys.put(realm, new Key(kid, keyPair, algorithm, rawValidFrom));
            }
        }

        // find youngest valid private key for each realm
        final Map<String,Signer> newSigners = new HashMap<>();
        final int now = (int)(System.currentTimeMillis() / 1000);
        for (String realm : realmKeys.keySet()) {
            final Collection<Key> keys = realmKeys.get(realm);
            final Optional<Key> key = keys.stream()
                    .filter(k -> k.getValidFrom() < now) // kicks out invalid (future) keys
                    .sorted() // sorts youngest first
                    .findFirst();

            if (key.isPresent()) {
                // use this signer for this realm
                Key k = key.get();
                JWSSigner signer;
                switch (k.getAlgorithm().getName()) {
                    case "ES256":
                    case "ES384":
                    case "ES512":
                        signer = new ECDSASigner((ECPrivateKey) k.getKeyPair().getPrivate());
                        break;
                    default:
                        signer = new RSASSASigner((RSAPrivateKey) k.getKeyPair().getPrivate());
                }
                newSigners.put(realm, new Signer(k.getKid(), signer, k.getAlgorithm()));
            }
        }

        // swap the current information, forget about all other private keys
        LOG.info("New key configuration:");
        for (Map.Entry<String,Signer> signer: newSigners.entrySet()) {
            LOG.info("Signing key for realm {}: {} ({}).", signer.getKey(), signer.getValue().getKid(),
                    signer.getValue().getAlgorithm().getName());
        }
        for (JWK jwk: newPublicKeys) {
            LOG.info("Public key {} ({}) available.", jwk.getKeyID(), jwk.getAlgorithm().getName());
        }

        this.currentPublicKeys.set(Collections.unmodifiableList(newPublicKeys));
        this.currentSigner.set(Collections.unmodifiableMap(newSigners));

        LOG.info("Key configuration updated.");
    }

    private JWK createRSAJWK(final String kid, final PublicKey publicKey, final JWSAlgorithm algorithm) {
        return new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(algorithm)
                .build();
    }

    private JWK createECJWK(final String kid, final PublicKey publicKey, final ECKey.Curve curve, final JWSAlgorithm algorithm) {
        return new ECKey.Builder(curve, (ECPublicKey) publicKey)
                .keyID(kid)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(algorithm)
                .build();
    }

    public static class Signer {
        private final String kid;
        private final JWSSigner signer;
        private final JWSAlgorithm algorithm;

        private Signer(String kid, final JWSSigner signer, final JWSAlgorithm algorithm) {
            this.kid = kid;
            this.signer = signer;
            this.algorithm = algorithm;
        }

        public String getKid() {
            return kid;
        }

        public JWSSigner getJWSSigner() {
            return signer;
        }

        public JWSAlgorithm getAlgorithm() {
            return algorithm;
        }
    }

    private static class Key implements Comparable<Key> {
        private final String kid;
        private final KeyPair keyPair;
        private final JWSAlgorithm algorithm;
        private final int validFrom;

        private Key(String kid, KeyPair keyPair, JWSAlgorithm algorithm, int validFrom) {
            this.kid = kid;
            this.keyPair = keyPair;
            this.algorithm = algorithm;
            this.validFrom = validFrom;
        }

        public String getKid() {
            return kid;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public JWSAlgorithm getAlgorithm() {
            return algorithm;
        }

        public long getValidFrom() {
            return validFrom;
        }

        @Override
        public int compareTo(final Key key) {
            // youngest first
            return key.validFrom - validFrom;
        }
    }
}
