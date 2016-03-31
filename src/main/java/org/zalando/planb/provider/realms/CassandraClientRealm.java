package org.zalando.planb.provider.realms;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.zalando.planb.provider.*;

import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
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
    private static final String DEFAULT_SCOPES = "default_scopes";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String REDIRECT_URIS = "redirect_uris";
    private static final String CREATED_BY = "created_by";
    private static final String LAST_MODIFIED_BY = "last_modified_by";

    @Autowired
    private Session session;

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private CurrentUser currentUser;

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

    private void prepareStatements() {
        findOne = session.prepare(select()
                .column(CLIENT_SECRET_HASH)
                .column(SCOPES)
                .column(DEFAULT_SCOPES)
                .column(IS_CONFIDENTIAL)
                .column(NAME)
                .column(DESCRIPTION)
                .column(REDIRECT_URIS)
                .column(CREATED_BY)
                .column(LAST_MODIFIED_BY)
                .from(CLIENT)
                .where(eq(CLIENT_ID, bindMarker(CLIENT_ID)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(CLIENT)
                .value(CLIENT_ID, bindMarker(CLIENT_ID))
                .value(REALM, realmName)
                .value(CLIENT_SECRET_HASH, bindMarker(CLIENT_SECRET_HASH))
                .value(SCOPES, bindMarker(SCOPES))
                .value(DEFAULT_SCOPES, bindMarker(DEFAULT_SCOPES))
                .value(IS_CONFIDENTIAL, bindMarker(IS_CONFIDENTIAL))
                .value(NAME, bindMarker(NAME))
                .value(DESCRIPTION, bindMarker(DESCRIPTION))
                .value(REDIRECT_URIS, bindMarker(REDIRECT_URIS))
                .value(CREATED_BY, bindMarker(CREATED_BY))
                .value(LAST_MODIFIED_BY, bindMarker(LAST_MODIFIED_BY)))
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
    public void update(String clientId, ClientData data) throws NotFoundException {
        final ClientData existing = get(clientId).orElseThrow(() -> new NotFoundException(format("Could not find client %s in realm %s", clientId, getName())));

        session.execute(upsert.bind()
                .setString(CLIENT_ID, clientId)
                .setString(CLIENT_SECRET_HASH, Optional.ofNullable(data.getClientSecretHash()).orElseGet(existing::getClientSecretHash))
                .setSet(SCOPES, Optional.ofNullable(data.getScopes()).filter(set -> !set.isEmpty()).orElseGet(existing::getScopes))
                .setSet(DEFAULT_SCOPES, Optional.ofNullable(data.getDefaultScopes()).filter(set -> !set.isEmpty()).orElseGet(existing::getDefaultScopes))
                .setBool(IS_CONFIDENTIAL, Optional.ofNullable(data.getConfidential()).orElseGet(existing::getConfidential))
                .setString(NAME, Optional.ofNullable(data.getName()).orElseGet(existing::getName))
                .setString(DESCRIPTION, Optional.ofNullable(data.getDescription()).orElseGet(existing::getDescription))
                .setSet(REDIRECT_URIS, Optional.ofNullable(data.getRedirectUris()).filter(set -> !set.isEmpty()).orElseGet(existing::getRedirectUris))
                .setString(CREATED_BY, existing.getCreatedBy())
                .setString(LAST_MODIFIED_BY, currentUser.get()));
    }

    @Override
    public void delete(String clientId) {
        get(clientId).orElseThrow(() -> new NotFoundException(format("Could not find client %s in realm %s", clientId, realmName)));

        session.execute(deleteOne.bind().setString(CLIENT_ID, clientId));
    }

    @Override
    public void createOrReplace(String clientId, ClientData client) {
        final Optional<String> existingCreatedBy = get(clientId).map(ClientData::getCreatedBy);

        session.execute(upsert.bind()
                .setString(CLIENT_ID, clientId)
                .setString(CLIENT_SECRET_HASH, client.getClientSecretHash())
                .setSet(SCOPES, newHashSet(client.getScopes()))
                .setSet(DEFAULT_SCOPES, newHashSet(client.getDefaultScopes()))
                .setBool(IS_CONFIDENTIAL, client.getConfidential())
                .setString(NAME, client.getName())
                .setString(DESCRIPTION, client.getDescription())
                .setSet(REDIRECT_URIS, newHashSet(client.getRedirectUris()))
                .setString(CREATED_BY, existingCreatedBy.orElseGet(currentUser))
                .setString(LAST_MODIFIED_BY, currentUser.get()));
    }

    @Override
    public Optional<ClientData> get(String clientId) {
        return Optional.ofNullable(findOne.bind().setString(CLIENT_ID, clientId))
                .map(session::execute)
                .map(ResultSet::one)
                .map(CassandraClientRealm::toClient);
    }

    private static ClientData toClient(Row row) {
        return ClientData.builder()
                .clientSecretHash(row.getString(CLIENT_SECRET_HASH))
                .scopes(row.getSet(SCOPES, String.class))
                .defaultScopes(row.getSet(DEFAULT_SCOPES, String.class))
                .confidential(row.getBool(IS_CONFIDENTIAL))
                .name(row.getString(NAME))
                .description(row.getString(DESCRIPTION))
                .redirectUris(row.getSet(REDIRECT_URIS, String.class))
                .createdBy(row.getString(CREATED_BY))
                .lastModifiedBy(row.getString(LAST_MODIFIED_BY))
                .build();
    }
}
