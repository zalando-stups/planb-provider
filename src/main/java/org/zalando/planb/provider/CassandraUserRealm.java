package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.mapping.MappingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

@Component
@Scope("prototype")
public class CassandraUserRealm implements UserManagedRealm {

    private static final String USER = "user";
    private static final String USERNAME = "username";
    private static final String REALM = "realm";
    private static final String PASSWORD_HASHES = "password_hashes";
    private static final String SCOPES = "scopes";
    private static final String CREATED_BY = "created_by";
    private static final String LAST_MODIFIED_BY = "last_modified_by";

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private Session session;

    @Autowired
    private CurrentUser currentUser;

    private String realmName;

    private PreparedStatement findOne;
    private PreparedStatement deleteOne;
    private PreparedStatement upsert;
    private PreparedStatement addPassword;

    private void prepareStatements() {
        findOne = session.prepare(select()
                .column(PASSWORD_HASHES)
                .column(SCOPES)
                .column(CREATED_BY)
                .column(LAST_MODIFIED_BY)
                .from(USER)
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(USER)
                .value(USERNAME, bindMarker(USERNAME))
                .value(REALM, realmName)
                .value(PASSWORD_HASHES, bindMarker(PASSWORD_HASHES))
                .value(SCOPES, bindMarker(SCOPES))
                .value(CREATED_BY, bindMarker(CREATED_BY))
                .value(LAST_MODIFIED_BY, bindMarker(LAST_MODIFIED_BY)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        deleteOne = session.prepare(QueryBuilder.delete().all()
                .from(USER)
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());

        addPassword = session.prepare(QueryBuilder.update(USER)
                .with(addAll(PASSWORD_HASHES, bindMarker(PASSWORD_HASHES)))
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getWriteConsistencyLevel());
    }

    @Override
    public void initialize(String realmName) {
        Assert.hasText(realmName, "realmName must not be blank");
        this.realmName = realmName;
        new MappingManager(session).udtCodec(UserPasswordHash.class);

        prepareStatements();
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public void update(String username, UserData data) throws NotFoundException {
        final UserData existing = get(username).orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())));

        session.execute(upsert.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, Optional.ofNullable(data.getPasswordHashes()).filter(list -> !list.isEmpty()).map(this::withAuditing).orElseGet(existing::getPasswordHashes))
                .setMap(SCOPES, Optional.ofNullable(data.getScopes()).filter(scopes -> !scopes.isEmpty()).orElseGet(existing::getScopes))
                .setString(CREATED_BY, existing.getCreatedBy())
                .setString(LAST_MODIFIED_BY, currentUser.get()));
    }

    @Override
    public void delete(String username) throws NotFoundException {
        assertExists(username);
        session.execute(deleteOne.bind().setString(USERNAME, username));
    }

    @Override
    public void createOrReplace(String username, UserData user) {
        final Optional<String> existingCreatedBy = get(username).map(UserData::getCreatedBy);
        session.execute(upsert.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, withAuditing(user.getPasswordHashes()))
                .setMap(SCOPES, user.getScopes())
                .setString(CREATED_BY, existingCreatedBy.orElseGet(currentUser))
                .setString(LAST_MODIFIED_BY, currentUser.get()));
    }

    @Override
    public void addPassword(String username, UserPasswordHash password) {
        assertExists(username);
        session.execute(addPassword.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, singleton(withAuditing(password))));
    }

    @Override
    public Optional<UserData> get(String username) {
        return Optional.of(findOne.bind().setString(USERNAME, username))
                .map(session::execute)
                .map(ResultSet::one)
                .map(this::toUser);
    }

    private Set<UserPasswordHash> withAuditing(Set<UserPasswordHash> set) {
        return set.stream().map(this::withAuditing).collect(toSet());
    }

    private UserPasswordHash withAuditing(UserPasswordHash userPasswordHash) {
        userPasswordHash.setCreated((int) ZonedDateTime.now().toEpochSecond());
        userPasswordHash.setCreatedBy(currentUser.get());
        return userPasswordHash;
    }

    private void assertExists(String username) {
        get(username).orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())));
    }

    private UserData toUser(Row row) {
        return new UserData.Builder()
                .withPasswordHashes(row.getSet(PASSWORD_HASHES, UserPasswordHash.class))
                .withScopes(row.getMap(SCOPES, String.class, String.class))
                .withCreatedBy(row.getString(CREATED_BY))
                .withLastModifiedBy(row.getString(LAST_MODIFIED_BY))
                .build();
    }
}
