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
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.patch;
import static org.springframework.http.RequestEntity.put;


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

    @Test
    public void testDeleteInNotManagedRealm() throws Exception {
        try {
            restTemplate.delete(URI.create("http://localhost:" + port + "/clients/animals/1"));
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
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
                .value("is_confidential", true));
        assertThat(fetchClient("0815", "/services")).isNotNull();

        restTemplate.delete(URI.create("http://localhost:" + port + "/clients/services/0815"));

        assertThat(fetchClient("0815", "/services")).isNull();
    }

    @Test
    public void testDeleteServicesClientNotFound() throws Exception {
        try {
            restTemplate.delete(URI.create("http://localhost:" + port + "/clients/services/not-found"));
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testCreateAndReplaceClient() throws Exception {
        final URI uri = URI.create("http://localhost:" + port + "/clients/customers/4711");

        // check that client doesn't exist before
        assertThat(fetchClient("4711", "/customers")).isNull();

        final Client body1 = new Client();
        body1.setSecretHash("abc");
        body1.setScopes(asList("read_foo", "read_bar"));
        body1.setIsConfidential(true);

        // create the client
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).body(body1), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchClient("4711", "/customers")).isNotNull().has(valuesEqualTo(body1));

        final Client body2 = new Client();
        body2.setSecretHash("xyz");
        body2.setScopes(asList("read_team", "write_hello", "write_world"));
        body2.setIsConfidential(false);

        // update the client. modify all (non-key) columns
        assertThat(restTemplate.exchange(put(uri).contentType(APPLICATION_JSON).body(body2), Void.class)
                .getStatusCode())
                .isEqualTo(OK);

        assertThat(fetchClient("4711", "/customers")).isNotNull().has(valuesEqualTo(body2));
    }

    @Test
    public void testUpdateServicesClientNotFound() throws Exception {
        try {
            final URI uri = URI.create("http://localhost:" + port + "/clients/services/not-found");
            restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).body(new Client()), Void.class);
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (final HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }

    @Test
    public void testUpdateClient() throws Exception {
        // given an existing client
        session.execute(insertInto("client")
                .value("client_id", "1234")
                .value("realm", "/services")
                .value("client_secret_hash", "qwertz")
                .value("scopes", newHashSet("foo", "bar"))
                .value("is_confidential", true));

        final Client service1234 = new Client();
        service1234.setSecretHash("qwertz");
        service1234.setScopes(asList("foo", "bar"));
        service1234.setIsConfidential(true);

        final URI uri = URI.create("http://localhost:" + port + "/clients/services/1234");

        // when the secretHash is updated
        final String newSecretHash = "llsdflhsdhjdjoj345";
        final Client body1 = new Client();
        body1.setSecretHash(newSecretHash);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).body(body1), Void.class);

        // then changes only this change is reflected in data storage
        service1234.setSecretHash(newSecretHash);
        assertThat(fetchClient("1234", "/services")).has(valuesEqualTo(service1234));

        // when the scopes are updated
        final List<String> newScopes = asList("mickey", "mouse", "donald", "duck");
        final Client body2 = new Client();
        body2.setScopes(newScopes);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).body(body2), Void.class);

        // then this change is also reflected in data storage
        service1234.setScopes(newScopes);
        assertThat(fetchClient("1234", "/services")).has(valuesEqualTo(service1234));

        // and when finally the confidential flag is updated
        final Client body3 = new Client();
        body3.setIsConfidential(false);
        restTemplate.exchange(patch(uri).contentType(APPLICATION_JSON).body(body3), Void.class);

        // then this change is also reflected in data storage
        service1234.setIsConfidential(false);
        assertThat(fetchClient("1234", "/services")).has(valuesEqualTo(service1234));
    }

    private Condition<? super Row> valuesEqualTo(Client expected) {
        return allOf(
                new Condition<>(r -> Objects.equals(r.getString("client_secret_hash"), expected.getSecretHash()), "client_secret_hash = %s", expected.getSecretHash()),
                new Condition<>(r -> Objects.equals(r.getBool("is_confidential"), expected.getIsConfidential()), "is_confidential = %s", expected.getIsConfidential()),
                new Condition<>(r -> Objects.equals(r.getSet("scopes", String.class), newHashSet(expected.getScopes())), "scopes = %s", expected.getScopes()));
    }

    private Row fetchClient(String clientId, String realm) {
        return session
                .execute(select().all().from("client").where(eq("client_id", clientId)).and(eq("realm", realm)))
                .one();
    }
}
