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
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.singleton;

@Component
@Scope("prototype")
public class CassandraUserRealm implements UserManagedRealm {

    private static final String USER = "user";
    private static final String USERNAME = "username";
    private static final String REALM = "realm";
    private static final String PASSWORD_HASHES = "password_hashes";
    private static final String SCOPES = "scopes";

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private Session session;

    private String realmName;

    private PreparedStatement selectUser;
    private PreparedStatement findOne;
    private PreparedStatement deleteOne;
    private PreparedStatement upsert;
    private PreparedStatement addPassword;

    void prepareStatements() {
        selectUser = session.prepare(
                select("password_hashes")
                        .from(cassandraProperties.getKeyspace(), USER)
                        // TODO also match with this.realmName
                        .where(eq(USERNAME, bindMarker(USERNAME))));

        findOne = session.prepare(select().all()
                .from(USER)
                .where(eq(USERNAME, bindMarker(USERNAME)))
                .and(eq(REALM, realmName)))
                .setConsistencyLevel(cassandraProperties.getReadConsistencyLevel());

        upsert = session.prepare(insertInto(USER)
                .value(USERNAME, bindMarker(USERNAME))
                .value(REALM, realmName)
                .value(PASSWORD_HASHES, bindMarker(PASSWORD_HASHES))
                .value(SCOPES, bindMarker(SCOPES)))
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
        prepareStatements();
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public Map<String, Object> authenticate(final String username, final String password, final String[] scopes)
            throws RealmAuthenticationException {

        // selectUser to figure out password


        final ResultSet result = session.execute(selectUser.bind().setString(USERNAME, username));
        final Row row = result.one();
        final Set<String> passwordHashes = row.getSet("password_hashes", String.class);

        // TODO put password_hash to the query and find

        return new HashMap<String, Object>() {{
            put("sub", username);
        }};
    }

    @Override
    public void delete(String username) throws NotFoundException {
        assertExists(username);
        session.execute(deleteOne.bind().setString(USERNAME, username));
    }

    @Override
    public void createOrReplace(String username, User user) {
        session.execute(upsert.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, newHashSet(user.getPasswordHashes()))
                .setMap(SCOPES, scopesMap(user)));
    }

    // TODO is it possible to use Map in the User POJO?
    private Map<String, String> scopesMap(User user) {
        final Map<?, ?> scopes = (Map<?, ?>) user.getScopes();
        final Map<String, String> result = newHashMapWithExpectedSize(scopes.size());
        scopes.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
        return result;
    }

    @Override
    public void addPassword(String username, Password password) {
        assertExists(username);
        session.execute(addPassword.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, singleton(password.getPasswordHash())));
    }

    @Override
    public Optional<User> get(String username) {
        return Optional.of(findOne.bind().setString(USERNAME, username))
                .map(session::execute)
                .map(ResultSet::one)
                .map(this::toUser);
    }

    private void assertExists(String username) {
        get(username).orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())));
    }

    private User toUser(Row row) {
        final User user = new User();
        user.setPasswordHashes(newArrayList(row.getSet(PASSWORD_HASHES, String.class)));
        user.setScopes(row.getMap(SCOPES, String.class, String.class));
        return user;
    }
}
