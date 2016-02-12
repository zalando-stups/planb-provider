package org.zalando.planb.provider;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.zalando.planb.provider.api.Client;

import java.net.URI;


@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class ClientsApiControllerIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    @Test
    public void invoke() {
        RestTemplate restTemplate = new RestTemplate();
        Client body = new Client();
        body.setScopes(Lists.newArrayList("one", "two"));
        body.setSecretHash("secret_hash");
        RequestEntity<?> request = RequestEntity.put(URI.create("http://localhost:" + port + "/clients/testRealm/13"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);

        ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}
