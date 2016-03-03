package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static java.time.ZonedDateTime.now;

/**
 * Created by hjacobs on 3/3/16.
 */
@Component
public class CassandraAuthorizationCodeService {

    private static final String AUTHORIZATION_CODE = "authorization_code";

    private static final String CODE = "code";
    private static final String STATE = "state";
    private static final String CLIENT_ID = "client_id";
    private static final String REALM = "realm";
    private static final String SCOPES = "scopes";
    private static final String EXPIRES = "expires";

    private static final Duration TTL = Duration.ofMinutes(10);

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
        findOne = session.prepare(select().all()
                .from(AUTHORIZATION_CODE)
                .where(eq(CODE, bindMarker(CODE))))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(AUTHORIZATION_CODE)
                .value(CODE, bindMarker(CODE))
                .value(STATE, bindMarker(STATE))
                .value(CLIENT_ID, bindMarker(CLIENT_ID))
                .value(REALM, bindMarker(REALM))
                .value(SCOPES, bindMarker(SCOPES))
                .value(EXPIRES, bindMarker(EXPIRES)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        deleteOne = session.prepare(QueryBuilder.delete().all()
                .from(AUTHORIZATION_CODE)
                .where(eq(CODE, bindMarker(CODE))))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }

    public String create(String state, String clientId, String realm, Set<String> scopes) {

        String code = UUID.randomUUID().toString();

        int expires = (int) now().plus(TTL).toEpochSecond();
        session.execute(upsert.bind()
                .setString(CODE, code)
                .setString(STATE, state)
                .setString(CLIENT_ID, clientId)
                .setString(REALM, realm)
                .setSet(SCOPES, scopes)
                .setInt(EXPIRES, expires));

        return code;
    }
}
