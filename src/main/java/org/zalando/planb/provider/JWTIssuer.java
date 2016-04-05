package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import net.minidev.json.JSONStyle;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.realms.Realm;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class JWTIssuer {

    private final Logger log = getLogger(getClass());

    // we just need one char to identify ourselves as "Plan B Provider" (Base64 has 33% overhead)
    private static final String ISSUER = "B";
    private static final String AUTHORIZED_PARTY = "azp";

    @Autowired
    private RealmProperties realmProperties;

    @Autowired
    private OIDCKeyHolder keyHolder;

    @Autowired
    private MetricRegistry metricRegistry;

    static String getSignedJWT(JWTClaimsSet claims, OIDCKeyHolder.Signer signer) throws JOSEException {
        final JWSAlgorithm algorithm = signer.getAlgorithm();

        // NOTE: we are doing the JSON serialization "by hand" here to use the correct compression flag
        // (the default is using net.minidev.json.JStylerObj.ESCAPE4Web which also escapes forward slashes)
        final String serializedJson = claims.toJSONObject().toJSONString(JSONStyle.LT_COMPRESS);
        final JWSHeader header = new JWSHeader(algorithm, null, null, null, null, null, null, null, null, null,
                signer.getKid(), null, null);
        final Payload payload = new Payload(serializedJson);
        final JWSObject jwt = new JWSObject(header, payload);

        jwt.sign(signer.getJWSSigner());

        return jwt.serialize();
    }


    /**
     * Issue (create) a single JWT access token
     * @param userRealm the realm
     * @param clientId the OAuth client ID
     * @param scopes granted scopes
     * @param claims JWT claim values
     * @return raw JWT string
     * @throws JOSEException
     */
    public String issueAccessToken(Realm userRealm, String clientId, Set<String> scopes, Map<String, String> claims) throws JOSEException {
        // this should never happen (only if some realm does not return "sub"
        Preconditions.checkState(claims.containsKey(Realm.SUB), "'sub' claim missing");
        final long tokenLifetimeInMilliseconds = realmProperties.getTokenLifetime(userRealm.getName()).toMillis();
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .expirationTime(new Date(System.currentTimeMillis() + tokenLifetimeInMilliseconds))
                .issueTime(new Date())
                .claim("realm", userRealm.getName())
                .claim("scope", scopes);
        claims.forEach(claimsBuilder::claim);
        if (scopes.contains(AUTHORIZED_PARTY)) {
            // http://openid.net/specs/openid-connect-core-1_0.html#IDToken
            // Authorized party - the party to which the ID Token was issued.
            // If present, it MUST contain the OAuth 2.0 Client ID of this party.
            claimsBuilder.claim(AUTHORIZED_PARTY, clientId);
        }
        final JWTClaimsSet jwtClaims = claimsBuilder.build();

        // sign JWT
        OIDCKeyHolder.Signer signer = keyHolder.getCurrentSigner(userRealm.getName())
                .orElseThrow(() -> new UnsupportedOperationException("No key found for signing requests of realm " + userRealm.getName()));

        final Metric signingMetric = new Metric(metricRegistry).start();
        String rawJWT;
        try {
            rawJWT = getSignedJWT(jwtClaims, signer);
        } finally {
            signingMetric.finish("planb.provider.jwt.signing." + signer.getAlgorithm().getName());
        }

        final String maskedSubject = userRealm.maskSubject(claims.get(Realm.SUB));
        log.info("Issued JWT for '{}' requested by client {}/{}", maskedSubject, userRealm.getName(), clientId);
        return rawJWT;
    }
}
