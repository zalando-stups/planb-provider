package org.zalando.planb.provider;

import com.datastax.driver.core.Session;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.zalando.planb.provider.api.Client;

import javax.annotation.PostConstruct;
import java.net.URI;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.failBecauseExceptionWasNotThrown;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.put;


@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class ClientControllerIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private Session session;

    @PostConstruct
    public void setUp() throws Exception {
        session.execute(insertInto("client")
                .value("client_id", "0815")
                .value("realm", "/services")
                .value("client_secret_hash", "qwertz")
                .value("scopes", newHashSet("foo", "bar"))
                .value("is_confidential", true));
    }

    @Test
    public void invoke() {
        final Client body = new Client();
        body.setScopes(Lists.newArrayList("one", "two"));
        body.setSecretHash("secret_hash");

        final RequestEntity<?> request = put(URI.create("http://localhost:" + port + "/clients/testRealm/13"))
                .contentType(APPLICATION_JSON)
                .body(body);

        final ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
    }

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
        assertThat(session.execute(
                select().all()
                        .from("client")
                        .where(eq("client_id", "0815"))
                        .and(eq("realm", "/services"))).one())
                .isNotNull();

        restTemplate.delete(URI.create("http://localhost:" + port + "/clients/services/0815"));

        assertThat(session.execute(
                select().all()
                        .from("client")
                        .where(eq("client_id", "0815"))
                        .and(eq("realm", "/services"))).one())
                .isNull();
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
}
