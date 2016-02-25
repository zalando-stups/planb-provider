package org.zalando.planb.provider;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.schemabuilder.UDTType;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.UDTMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    private UDTMapper<UserPasswordHash> mapper;

    private String realmName;

    private PreparedStatement findOne;
    private PreparedStatement deleteOne;
    private PreparedStatement upsert;
    private PreparedStatement addPassword;

    private void prepareStatements() {
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

        // On UDTs:
        // http://www.datastax.com/dev/blog/cql-in-2-1
        // https://docs.datastax.com/en/developer/java-driver/2.1/java-driver/reference/mappingUdts.html
        this.mapper = new MappingManager(session).udtMapper(UserPasswordHash.class);

        prepareStatements();
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public void delete(String username) throws NotFoundException {
        assertExists(username);
        session.execute(deleteOne.bind().setString(USERNAME, username));
    }

    @Override
    public void createOrReplace(String username, User user) {
        Set<UDTValue> udtValues = user.getPasswordHashes().stream()
                .map(hash -> new UserPasswordHash(hash, "todo"))
                .map(mapper::toUDT).collect(Collectors.toSet());

        session.execute(upsert.bind()
                .setString(USERNAME, username)
                .setSet(PASSWORD_HASHES, udtValues)
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
                .setSet(PASSWORD_HASHES, singleton(mapper.toUDT(new UserPasswordHash(password.getPasswordHash(), "todo")))));
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
        List<String> passwordHashes = row.getSet(PASSWORD_HASHES, UDTValue.class).stream()
                .map(mapper::fromUDT)
                .map(UserPasswordHash::getPasswordHash)
                .collect(Collectors.toList());
        user.setPasswordHashes(passwordHashes);
        user.setScopes(row.getMap(SCOPES, String.class, String.class));
        return user;
    }
}
