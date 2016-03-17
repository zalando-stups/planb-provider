package org.zalando.planb.provider;

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

@Component
public class CassandraConsentService {

    private static final String CONSENT = "consent";

    private static final String CLIENT_ID = "client_id";
    private static final String REALM = "realm";
    private static final String SCOPES = "scopes";
    private static final String USERNAME = "username";

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
                .column(SCOPES)
                .from(CONSENT)
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, bindMarker(REALM)))
                .and(eq(CLIENT_ID, bindMarker(CLIENT_ID))))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(CONSENT)
                .value(USERNAME, bindMarker(USERNAME))
                .value(REALM, bindMarker(REALM))
                .value(CLIENT_ID, bindMarker(CLIENT_ID))
                .value(SCOPES, bindMarker(SCOPES)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        deleteOne = session.prepare(delete()
                .all()
                .from(CONSENT)
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, bindMarker(REALM)))
                .and(eq(CLIENT_ID, bindMarker(CLIENT_ID))))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }

    public void store(final String username, final String realm, final String clientId, final Set<String> scopes) {

        session.execute(upsert.bind()
                .setString(USERNAME, username)
                .setString(REALM, realm)
                .setString(CLIENT_ID,clientId).setSet(SCOPES, scopes));
    }

    public Set<String> getConsentedScopes(final String username, final String realm, final String clientId) {

        return Optional.ofNullable(findOne.bind()
                .setString(USERNAME, username)
                .setString(REALM, realm)
                .setString(CLIENT_ID, clientId))
                .map(session::execute)
                .map(ResultSet::one)
                .map(r -> r.getSet(SCOPES, String.class))
                .orElse(Collections.emptySet());
    }

    public void withdraw(final String username, final String realm, final String clientId) {
        session.execute(deleteOne.bind()
                .setString(USERNAME, username)
                .setString(REALM, realm)
                .setString(CLIENT_ID, clientId));
    }

}
