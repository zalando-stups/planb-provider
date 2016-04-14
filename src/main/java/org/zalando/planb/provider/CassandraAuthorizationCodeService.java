package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static java.time.ZonedDateTime.now;

@Component
public class CassandraAuthorizationCodeService {

    private static final String AUTHORIZATION_CODE = "authorization_code";

    private static final String CODE = "code";
    private static final String STATE = "state";
    private static final String CLIENT_ID = "client_id";
    private static final String REALM = "realm";
    private static final String SCOPES = "scopes";
    private static final String CLAIMS = "claims";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String EXPIRES = "expires";

    /**
     * http://tools.ietf.org/html/rfc6749#section-4.1.2 says:
     * The authorization code MUST expire shortly after it is issued to mitigate the risk of leaks.
     * A maximum authorization code lifetime of 10 minutes is RECOMMENDED.
     * 60 seconds sounds like enough time to complete the authorization code grant flow.
     */
    private static final Duration LIFETIME = Duration.ofSeconds(60);
    // clean up: automatically remove non-used authorization_code rows after 15 minutes
    private static final int TTL = (int) Duration.ofMinutes(15).getSeconds();

    @Autowired
    private Session session;

    @Autowired
    private CassandraProperties cassandraProperties;

    private PreparedStatement deleteOne;
    private PreparedStatement findOne;
    private PreparedStatement upsert;

    @PostConstruct
    public void initialize() {
        prepareStatements();
    }

    private void prepareStatements() {
        findOne = session.prepare(select()
                .column(CODE)
                .column(STATE)
                .column(CLIENT_ID)
                .column(REALM)
                .column(SCOPES)
                .column(CLAIMS)
                .column(REDIRECT_URI)
                .column(EXPIRES)
                .from(AUTHORIZATION_CODE)
                .where(eq(CODE, bindMarker(CODE))))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(AUTHORIZATION_CODE).using(ttl(TTL))
                .value(CODE, bindMarker(CODE))
                .value(STATE, bindMarker(STATE))
                .value(CLIENT_ID, bindMarker(CLIENT_ID))
                .value(REALM, bindMarker(REALM))
                .value(SCOPES, bindMarker(SCOPES))
                .value(CLAIMS, bindMarker(CLAIMS))
                .value(REDIRECT_URI, bindMarker(REDIRECT_URI))
                .value(EXPIRES, bindMarker(EXPIRES)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        deleteOne = session.prepare(QueryBuilder.delete().all()
                .from(AUTHORIZATION_CODE)
                .where(eq(CODE, bindMarker(CODE))))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }


    /**
     * Generate URL-safe authorization code string
     */
    static String getRandomCode() {
        // 24 Bytes will become 32 characters in Base64
        byte[] bytes = new byte[24];
        // NOTE: we don't need to use SecureRandom here as the secret is only very short-lived
        // "When applicable, use of ThreadLocalRandom rather than shared Random objects
        // in concurrent programs will typically encounter much less overhead and contention"
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public String create(String state, String clientId, String realm, Set<String> scopes, Map<String, String> claims, URI redirectUri) {
        String code = getRandomCode();

        int expires = (int) now().plus(LIFETIME).toEpochSecond();
        session.execute(upsert.bind()
                .setString(CODE, code)
                .setString(STATE, state)
                .setString(CLIENT_ID, clientId)
                .setString(REALM, realm)
                .setSet(SCOPES, scopes)
                .setMap(CLAIMS, claims)
                .setString(REDIRECT_URI, redirectUri.toString())
                .setInt(EXPIRES, expires));

        return code;
    }

    public Optional<AuthorizationCode> invalidate(String code) {
        Optional<AuthorizationCode> authorizationCode = Optional.ofNullable(findOne.bind().setString(CODE, code))
                .map(session::execute)
                .map(ResultSet::one)
                .filter(row -> row.getInt(EXPIRES) > now().toEpochSecond())
                .map(CassandraAuthorizationCodeService::toAuthorizationCode);
        // Delete the authorization code immediately.
        // http://tools.ietf.org/html/rfc6749#section-4.1.2 says:
        // "The client MUST NOT use the authorization code more than once."
        session.execute(deleteOne.bind().setString(CODE, code));
        return authorizationCode;
    }

    private static AuthorizationCode toAuthorizationCode(Row row) {
        return AuthorizationCode.builder()
                .code(row.getString(CODE))
                .state(row.getString(STATE))
                .clientId(row.getString(CLIENT_ID))
                .realm(row.getString(REALM))
                .scopes(row.getSet(SCOPES, String.class))
                .claims(row.getMap(CLAIMS, String.class, String.class))
                .redirectUri(URI.create(row.getString(REDIRECT_URI)))
                .build();
    }


}

