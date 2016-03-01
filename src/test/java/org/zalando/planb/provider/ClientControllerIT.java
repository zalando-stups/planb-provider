package org.zalando.planb.provider;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zalando.planb.provider.api.Client;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.allOf;
import static org.assertj.core.api.StrictAssertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.fail;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.delete;
import static org.springframework.http.RequestEntity.*;
import static org.springframework.http.RequestEntity.put;
import static org.zalando.planb.provider.ClientData.copyOf;


@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class ClientControllerIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    // use Apache HttpClient, because it supports the PATCH method
    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    @Autowired
    private Session session;

    private String basePath() {
        return "http://localhost:" + port + "/raw-sync";
    }

    private static String genHash(String pass) {
        return BCrypt.hashpw(pass, BCrypt.gensalt(4));
    }

    @Test
    public void testDeleteInNotManagedRealm() throws Exception {
        try {
            restTemplate.exchange(delete(URI.create(basePath() + "/clients/animals/1"))
                    .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    public void testDeleteUnauthorized() throws Exception {
        try {
            restTemplate.exchange(delete(URI.create(basePath() + "/clients/animals/1")).header(AUTHORIZATION, INVALID_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(UNAUTHORIZED);
        }
    }

    @Test
    public void testDeleteForbidden() throws Exception {
        try {
            restTemplate.exchange(delete(URI.create(basePath() + "/clients/animals/1")).header(AUTHORIZATION, INSUFFICIENT_SCOPES_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(FORBIDDEN);
        }
    }

    @Test
    public void testDeleteServicesClient() throws Exception {
        // given an existing client
        session.execute(insertInto("client")
                .value("client_id", "0815")
                .value("realm", "/services")
                .value("client_secret_hash", "qwertz")
                .value("scopes", newHashSet("foo", "bar"))
                .value("is_confidential", true)
                .value("created_by", USER1)
                .value("last_modified_by", USER2));
        assertThat(fetchClient("0815", "/services")).isNotNull();

        restTemplate.exchange(delete(URI.create(basePath() + "/clients/services/0815"))
                .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);

        assertThat(fetchClient("0815", "/services")).isNull();
    }

    @Test
    public void testDeleteServicesClientNotFound() throws Exception {
        try {
            restTemplate.exchange(delete(URI.create(basePath() + "/clients/services/not-found"))
                    .header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testCreateAndReplaceClientWrongHash() throws Exception {
        final URI uri = URI.create(basePath() + "/clients/customers/4710");

        // check that client doesn't exist before
        assertThat(fetchClient("4710", "/customers")).isNull();

        final Client body1 = new Client();
        body1.setSecretHash("wrong hash");
        body1.setScopes(asList("read_foo", "read_bar"));
        body1.setIsConfidential(true);

        try {
            restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body1), Void.class);
            fail("wrong BCrypt hash should fail with Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(BAD_REQUEST);
        }
    }

    @Test
    public void testCreateAndReplaceClient() throws Exception {
        final URI uri = URI.create(basePath() + "/clients/customers/4711");

        // check that client doesn't exist before
        assertThat(fetchClient("4711", "/customers")).isNull();

        final String hash = genHash("foo");

        final Client body1 = new Client();
        body1.setSecretHash(hash);
        body1.setScopes(asList("read_foo", "read_bar"));
        body1.setIsConfidential(true);

        // user1 creates the client
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body1), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchClient("4711", "/customers"))
                .isNotNull()
                .has(valuesEqualTo(copyOf(body1).withCreatedBy(USER1).withLastModifiedBy(USER1).build()));

        final Client body2 = new Client();
        body2.setSecretHash(hash);
        body2.setScopes(asList("read_team", "write_hello", "write_world"));
        body2.setIsConfidential(false);

        // user2 updates the client. modifying all (non-key) columns
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER2_ACCESS_TOKEN).body(body2), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchClient("4711", "/customers"))
                .isNotNull()
                .has(valuesEqualTo(copyOf(body2).withCreatedBy(USER1).withLastModifiedBy(USER2).build()));
    }

    @Test
    public void testUpdateServicesClientNotFound() throws Exception {
        try {
            final URI uri = URI.create(basePath() + "/clients/services/not-found");
            restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(new Client()), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testUpdateClient() throws Exception {
        final String hash = genHash("qwertz");

        // given an existing client
        session.execute(insertInto("client")
                .value("client_id", "1234")
                .value("realm", "/services")
                .value("client_secret_hash", hash)
                .value("scopes", newHashSet("foo", "bar"))
                .value("is_confidential", true)
                .value("created_by", USER1)
                .value("last_modified_by", USER2));

        final Client service1234 = new Client();
        service1234.setSecretHash(hash);
        service1234.setScopes(asList("foo", "bar"));
        service1234.setIsConfidential(true);

        final URI uri = URI.create(basePath() + "/clients/services/1234");

        // when the secretHash is updated
        final String newSecretHash = genHash("lolz");
        final Client body1 = new Client();
        body1.setSecretHash(newSecretHash);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body1), Void.class);

        // then changes only this change is reflected in data storage
        service1234.setSecretHash(newSecretHash);
        assertThat(fetchClient("1234", "/services"))
                .has(valuesEqualTo(copyOf(service1234).withCreatedBy(USER1).withLastModifiedBy(USER1).build()));

        // when the scopes are updated
        final List<String> newScopes = asList("mickey", "mouse", "donald", "duck");
        final Client body2 = new Client();
        body2.setScopes(newScopes);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER2_ACCESS_TOKEN).body(body2), Void.class);

        // then this change is also reflected in data storage
        service1234.setScopes(newScopes);
        assertThat(fetchClient("1234", "/services"))
                .has(valuesEqualTo(copyOf(service1234).withCreatedBy(USER1).withLastModifiedBy(USER2).build()));

        // and when finally the confidential flag is updated
        final Client body3 = new Client();
        body3.setIsConfidential(false);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).header(AUTHORIZATION, USER1_ACCESS_TOKEN).body(body3), Void.class);

        // then this change is also reflected in data storage
        service1234.setIsConfidential(false);
        assertThat(fetchClient("1234", "/services"))
                .has(valuesEqualTo(copyOf(service1234).withCreatedBy(USER1).withLastModifiedBy(USER1).build()));
    }

    private Condition<? super Row> valuesEqualTo(ClientData expected) {
        return allOf(
                new Condition<>(r -> Objects.equals(r.getString("client_secret_hash"), expected.getClientSecretHash()), "client_secret_hash = %s", expected.getClientSecretHash()),
                new Condition<>(r -> Objects.equals(r.getBool("is_confidential"), expected.isConfidential()), "is_confidential = %s", expected.isConfidential()),
                new Condition<>(r -> Objects.equals(r.getSet("scopes", String.class), newHashSet(expected.getScopes())), "scopes = %s", expected.getScopes()),
                new Condition<>(r -> Objects.equals(r.getString("created_by"), expected.getCreatedBy()), "created_by = %s", expected.getCreatedBy()),
                new Condition<>(r -> Objects.equals(r.getString("last_modified_by"), expected.getLastModifiedBy()), "last_modified_by = %s", expected.getLastModifiedBy()));
    }

    private Row fetchClient(String clientId, String realm) {
        return session
                .execute(select().all().from("client").where(eq("client_id", clientId)).and(eq("realm", realm)))
                .one();
    }
}
