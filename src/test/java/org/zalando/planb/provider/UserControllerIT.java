package org.zalando.planb.provider;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.delete;
import static org.springframework.http.RequestEntity.patch;
import static org.springframework.http.RequestEntity.post;
import static org.springframework.http.RequestEntity.put;
import static org.zalando.planb.provider.UserData.builderOf;
import static org.zalando.planb.provider.UserData.toUserPasswordHashSet;

@ActiveProfiles("it")
public class UserControllerIT extends AbstractOauthTest {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private Session session;

    private static String genHash(String pass) {
        return BCrypt.hashpw(pass, BCrypt.gensalt(4));
    }

    @Test
    public void testCreateAndReplaceUser() throws Exception {
        final URI uri = URI.create(getRawSyncBaseUri() + "/users/services/4711");

        // check that client doesn't exist before
        assertThat(fetchUser("4711", "/services")).isNull();

        final User body1 = new User();
        body1.setPasswordHashes(asList(genHash("abc"), genHash("def")));
        body1.setScopes(ImmutableMap.of(
                "uid", "4711",
                "write_all", "true"));

        // create the user
        assertThat(getRestTemplate().exchange(
                put(uri)
                        .contentType(APPLICATION_JSON)
                        .header(AUTHORIZATION, USER1_ACCESS_TOKEN)
                        .body(body1), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchUser("4711", "/services"))
                .isNotNull()
                .has(valuesEqualTo(builderOf(body1)
                        .createdBy(USER1)
                        .lastModifiedBy(USER1)
                        .passwordHashes(toUserPasswordHashSet(body1.getPasswordHashes(), USER1))
                        .build()));

        final User body2 = new User();
        body2.setPasswordHashes(asList(genHash("hello"), genHash("world")));
        body2.setScopes(singletonMap("write_all", "false"));

        // update the user. modify all (non-key) columns
        assertThat(getRestTemplate().exchange(put(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER2_ACCESS_TOKEN).body(body2), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchUser("4711", "/services"))
                .isNotNull()
                .has(valuesEqualTo(builderOf(body2)
                        .createdBy(USER1)
                        .lastModifiedBy(USER2)
                        .passwordHashes(toUserPasswordHashSet(body2.getPasswordHashes(), USER2))
                        .build()));
    }

    @Test
    public void testDeleteInNotManagedRealm() throws Exception {
        try {
            getRestTemplate().exchange(delete(URI.create(getRawSyncBaseUri() + "/users/animals/1"))
                    .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            Assertions.assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    public void testDeleteUnauthorized() throws Exception {
        try {
            getRestTemplate().exchange(delete(URI.create(getRawSyncBaseUri() + "/users/animals/1")).header(AUTHORIZATION, INVALID_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(UNAUTHORIZED);
        }
    }

    @Test
    public void testDeleteServicesClient() throws Exception {
        // given an existing user
        session.execute(insertInto("user")
                .value("username", "0815")
                .value("realm", "/services")
                .value("password_hashes", newHashSet(new UserPasswordHash("foo", "unknown"), new UserPasswordHash("bar", "unknown")))
                .value("scopes", singletonMap("write", "true"))
                .value("created_by", USER1)
                .value("last_modified_by", USER2));
        assertThat(fetchUser("0815", "/services")).isNotNull();

        getRestTemplate().exchange(delete(URI.create(getRawSyncBaseUri() + "/users/services/0815"))
                .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);

        assertThat(fetchUser("0815", "/services")).isNull();
    }

    @Test
    public void testDeleteUsersNotFound() throws Exception {
        try {
            getRestTemplate().exchange(delete(URI.create(getRawSyncBaseUri() + "/users/services/not-found"))
                    .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            Assertions.assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testUpdateUserNotFound() throws Exception {
        try {
            final URI uri = URI.create(getRawSyncBaseUri() + "/users/services/not-found");
            getRestTemplate().exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(new User()), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testUpdateUser() throws Exception {
        final Map<String, String> initialScopes = singletonMap("write", "true");

        // given an existing user
        session.execute(insertInto("user")
                .value("username", "1234")
                .value("realm", "/services")
                .value("password_hashes", newHashSet(new UserPasswordHash("foo", "unknown"), new UserPasswordHash("bar", "unknown")))
                .value("scopes", initialScopes)
                .value("created_by", USER1)
                .value("last_modified_by", USER1));

        final URI uri = URI.create(getRawSyncBaseUri() + "/users/services/1234");

        // when the password_hashes is updated
        final List<String> newPasswordHashes = asList(genHash("bar"), genHash("hello"), genHash("world"));
        final User body1 = new User();
        body1.setPasswordHashes(newPasswordHashes);
        getRestTemplate().exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER2_ACCESS_TOKEN).body(body1), Void.class);

        assertThat(fetchUser("1234", "/services"))
                .isNotNull()
                .has(valuesEqualTo(UserData.builder()
                        .scopes(initialScopes)
                        .passwordHashes(toUserPasswordHashSet(newPasswordHashes, USER2))
                        .createdBy(USER1)
                        .lastModifiedBy(USER2)
                        .build()));

        // when the scopes are updated
        final Map<String, String> newScopes = ImmutableMap.of(
                "uid", "mickey-mouse",
                "write_all", "true");
        final User body2 = new User();
        body2.setScopes(newScopes);
        getRestTemplate().exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body2), Void.class);

        assertThat(fetchUser("1234", "/services"))
                .isNotNull()
                .has(valuesEqualTo(UserData.builder()
                        .scopes(newScopes)
                        .passwordHashes(toUserPasswordHashSet(newPasswordHashes, USER2))
                        .createdBy(USER1)
                        .lastModifiedBy(USER1)
                        .build()));
    }

    @Test
    public void testAddPasswordWrongHash() throws Exception {
        // given an existing user
        session.execute(insertInto("user")
                .value("username", "testAddPasswordWrongHash")
                .value("realm", "/services")
                .value("password_hashes", singleton(new UserPasswordHash("foo", "test")))
                .value("scopes", singletonMap("write", "true"))
                .value("created_by", USER1)
                .value("last_modified_by", USER2));

        final URI uri = URI.create(getRawSyncBaseUri() + "/users/services/testAddPasswordWrongHash/password");
        final Password body = new Password();
        body.setPasswordHash("not a valid hash");
        try {
            getRestTemplate().exchange(post(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body), Void.class);
            fail("wrong BCrypt hash should fail with Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(BAD_REQUEST);
        }

        assertThat(fetchUser("testAddPasswordWrongHash", "/services").getSet("password_hashes", UserPasswordHash.class))
                .extracting("passwordHash", "createdBy").containsOnly(tuple("foo", "test"));
    }

    @Test
    public void testAddPassword() throws Exception {
        // given an existing user
        session.execute(insertInto("user")
                .value("username", "9876")
                .value("realm", "/services")
                .value("password_hashes", singleton(new UserPasswordHash("foo", "test")))
                .value("scopes", singletonMap("write", "true"))
                .value("created_by", USER1)
                .value("last_modified_by", USER2));

        final URI uri = URI.create(getRawSyncBaseUri() + "/users/services/9876/password");
        final Password body = new Password();
        String hash = genHash("bar");
        body.setPasswordHash(hash);
        assertThat(getRestTemplate().exchange(post(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body), Void.class)
                .getStatusCode())
                .isEqualTo(CREATED);
        assertThat(fetchUser("9876", "/services").getSet("password_hashes", UserPasswordHash.class))
                .extracting("passwordHash", "createdBy").containsOnly(tuple("foo", "test"), tuple(hash, USER1));
    }

    private Row fetchUser(String username, String realm) {
        return session
                .execute(select().all().from("user").where(eq("username", username)).and(eq("realm", realm)))
                .one();
    }

    private static Condition<? super Row> valuesEqualTo(UserData expected) {
        return allOf(
                new Condition<>(
                        r -> passwordHashesEqual(r.getSet("password_hashes", UserPasswordHash.class), expected.getPasswordHashes()),
                        "password_hashes = %s",
                        expected.getPasswordHashes()),
                new Condition<>(
                        r -> Objects.equals(r.getMap("scopes", String.class, String.class), expected.getScopes()),
                        "scopes = %s",
                        expected.getScopes()),
                new Condition<>(
                        r -> Objects.equals(r.getString("created_by"), expected.getCreatedBy()),
                        "created_by = %s",
                        expected.getCreatedBy()),
                new Condition<>(
                        r -> Objects.equals(r.getString("last_modified_by"), expected.getLastModifiedBy()),
                        "last_modified_by = %s",
                        expected.getLastModifiedBy()));
    }

    private static boolean passwordHashesEqual(Set<UserPasswordHash> actualPasswords, Set<UserPasswordHash> expectedPasswords) {
        return actualPasswords.size() == expectedPasswords.size()
                && expectedPasswords.stream()
                .allMatch(expected -> actualPasswords.stream()
                        .filter(actual -> Objects.equals(expected.getPasswordHash(), actual.getPasswordHash())
                                && Objects.equals(expected.getCreatedBy(), actual.getCreatedBy()))
                        .findFirst()
                        .isPresent());
    }
}
