package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.zalando.planb.provider.api.Client;

import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;

@Component
@Scope("prototype")
public class CassandraClientRealm implements ClientManagedRealm {

    private static final String REALM = "realm";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT = "client";
    private static final String CLIENT_SECRET_HASH = "client_secret_hash";
    private static final String IS_CONFIDENTIAL = "is_confidential";
    private static final String SCOPES = "scopes";

    @Autowired
    private Session session;

    @Autowired
    private CassandraProperties cassandraProperties;

    private String realmName;

    private PreparedStatement deleteOne;
    private PreparedStatement findOne;
    private PreparedStatement upsert;

    @Override
    public void initialize(String realmName) {
        Assert.hasText(realmName, "realmName must not be blank");
        this.realmName = realmName;
        prepareStatements();
    }

    /**
     * Shamelessly copied from schema.cql:
     * <pre>
     * CREATE TABLE provider.client (
     *     client_id TEXT,                  -- OAuth 2 client ID
     *     realm TEXT,                      -- Data pool this entity belongs to
     *     client_secret_hash TEXT,         -- Base64-encoded Bcrypt hash of the client secret
     *     scopes SET<TEXT>,                -- scopes this client is allowed to request
     *     is_confidential BOOLEAN,         -- whether the client is confidential or not (non-confidential clients should only be allowed to use the implicit flow)
     *     PRIMARY KEY ((client_id), realm)
     * );
     * </pre>
     */
    private void prepareStatements() {
        findOne = session.prepare(select().all()
                .from(CLIENT)
                .where(eq(CLIENT_ID, bindMarker(CLIENT_ID)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(CLIENT)
                .value(CLIENT_ID, bindMarker(CLIENT_ID))
                .value(REALM, realmName)
                .value(CLIENT_SECRET_HASH, bindMarker(CLIENT_SECRET_HASH))
                .value(SCOPES, bindMarker(SCOPES))
                .value(IS_CONFIDENTIAL, bindMarker(IS_CONFIDENTIAL)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        deleteOne = session.prepare(QueryBuilder.delete().all()
                .from(CLIENT)
                .where(eq(CLIENT_ID, bindMarker(CLIENT_ID)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public void delete(String clientId) {
        get(clientId).orElseThrow(() -> new NotFoundException(format("Could not find client %s in realm %s", clientId, realmName)));

        session.execute(deleteOne.bind().setString(CLIENT_ID, clientId));
    }

    @Override
    public void createOrReplace(String clientId, Client client) {
        session.execute(upsert.bind()
                .setString(CLIENT_ID, clientId)
                .setString(CLIENT_SECRET_HASH, client.getSecretHash())
                .setSet(SCOPES, newHashSet(client.getScopes()))
                .setBool(IS_CONFIDENTIAL, client.getIsConfidential()));
    }

    @Override
    public Optional<Client> get(String clientId) {
        return Optional.ofNullable(findOne.bind().setString(CLIENT_ID, clientId))
                .map(session::execute)
                .map(ResultSet::one)
                .map(CassandraClientRealm::toClient);
    }

    private static Client toClient(Row row) {
        final Client client = new Client();
        client.setSecretHash(row.getString(CLIENT_SECRET_HASH));
        client.setScopes(newArrayList(row.getSet(SCOPES, String.class)));
        client.setIsConfidential(row.getBool(IS_CONFIDENTIAL));
        return client;
    }
}
