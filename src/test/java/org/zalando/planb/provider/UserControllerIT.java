package org.zalando.planb.provider;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zalando.planb.provider.api.User;

import java.net.URI;
import java.util.Objects;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.StrictAssertions.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.put;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class UserControllerIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private Session session;

    // use Apache HttpClient, because it supports the PATCH method
    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());


    @Test
    public void testCreateAndReplaceUser() throws Exception {
        final URI uri = URI.create("http://localhost:" + port + "/users/services/4711");

        // check that client doesn't exist before
        assertThat(fetchUser("4711", "/services")).isNull();

        final User body1 = new User();
        body1.setPasswordHashes(asList("abc", "def"));
        body1.setScopes(ImmutableMap.of(
                "uid", "4711",
                "write_all", "true"));

        // create the user
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).body(body1), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchUser("4711", "/services")).isNotNull().has(valuesEqualTo(body1));

        final User body2 = new User();
        body2.setPasswordHashes(asList("hello", "world"));
        body2.setScopes(singletonMap("write_all", "false"));

        // update the user. modify all (non-key) columns
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).body(body2), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchUser("4711", "/services")).isNotNull().has(valuesEqualTo(body2));

    }

    @Test
    public void testDeleteInNotManagedRealm() throws Exception {
        try {
            restTemplate.delete(URI.create("http://localhost:" + port + "/users/animals/1"));
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            Assertions.assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    public void testDeleteServicesClient() throws Exception {
        // given an existing user
        session.execute(insertInto("user")
                .value("username", "0815")
                .value("realm", "/services")
                .value("password_hashes", newHashSet("foo", "bar"))
                .value("scopes", singletonMap("write", "true")));
        assertThat(fetchUser("0815", "/services")).isNotNull();

        restTemplate.delete(URI.create("http://localhost:" + port + "/users/services/0815"));

        assertThat(fetchUser("0815", "/services")).isNull();
    }

    @Test
    public void testDeleteUsersNotFound() throws Exception {
        try {
            restTemplate.delete(URI.create("http://localhost:" + port + "/users/services/not-found"));
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            Assertions.assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    private Condition<? super Row> valuesEqualTo(User expected) {
        return allOf(
                new Condition<>(
                        r -> Objects.equals(r.getSet("password_hashes", String.class), newHashSet(expected.getPasswordHashes())),
                        "password_hashes = %s",
                        expected.getPasswordHashes()),
                new Condition<>(
                        r -> Objects.equals(r.getMap("scopes", String.class, String.class), expected.getScopes()),
                        "scopes = %s",
                        expected.getScopes()));
    }

    private Row fetchUser(String username, String realm) {
        return session
                .execute(select().all().from("user").where(eq("username", username)).and(eq("realm", realm)))
                .one();
    }
}
