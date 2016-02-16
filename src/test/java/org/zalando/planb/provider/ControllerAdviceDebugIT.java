package org.zalando.planb.provider;

import java.net.URI;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it-debug")
public class ControllerAdviceDebugIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    RestTemplate restTemplate = new RestTemplate();

    @Test
    public void error() {
        RequestEntity request = RequestEntity.get(URI.create("http://localhost:" + port + "/throwError")).build();
        restTemplate.setErrorHandler(new PassThroughResponseErrorHandler());
        ResponseEntity<String> response = restTemplate.exchange(request, String.class);
        Assertions.assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        Assertions.assertThat(response.getBody()).contains("org.zalando.planb.provider.ErrorController.error");
        Assertions.assertThat(response.getBody()).contains("java.lang.RuntimeException");
    }

}
