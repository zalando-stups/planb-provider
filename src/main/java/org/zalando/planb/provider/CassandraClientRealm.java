package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

@Component
@Scope("prototype")
public class CassandraClientRealm implements ClientManagedRealm {

    private static final String REALM = "realm";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT = "client";

    private final Logger log = getLogger(getClass());

    @Autowired
    private Session session;
    @Autowired
    private CassandraProperties cassandraProperties;

    private String realmName;

    private PreparedStatement deleteOne;
    private PreparedStatement findOne;

    @Override
    public void initialize(String realmName) {
        Assert.hasText(realmName, "realmName must not be blank");
        this.realmName = realmName;
        prepareStatements();
    }

    private void prepareStatements() {
        findOne = session.prepare(select().all()
                .from(CLIENT)
                .where(eq(CLIENT_ID, bindMarker(CLIENT_ID)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        deleteOne = session.prepare(QueryBuilder.delete().all()
                .from(CLIENT)
                .where(eq(CLIENT_ID, bindMarker(CLIENT_ID)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }

    @Override
    public void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException {
        // TODO look up clientId in this.realmName and compare the clientSecret and scopes
    }

    @Override
    public void create() {

    }

    @Override
    public void update() {

    }

    @Override
    public void delete(String clientId) {
        final Optional<Row> client = Optional.ofNullable(
                session.execute(findOne.bind().setString(CLIENT_ID, clientId)).one());

        if (!client.isPresent()) {
            throw new NotFoundException(format("Could not find client %s in realm %s", clientId, realmName));
        }

        final ResultSet resultSet = session.execute(deleteOne.bind().setString(CLIENT_ID, clientId));
        log.info("ResultSet: {}", resultSet);
        resultSet.forEach(row -> log.info("{}", row));
    }
}
