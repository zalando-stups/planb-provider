package org.zalando.planb.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class PlanBProviderIT extends AbstractSpringTest {

    private static final Logger log = LoggerFactory.getLogger(PlanBProviderIT.class);

    @Value("${local.server.port}")
    private int port;

    RestTemplate rest = new RestTemplate();

    @Test
    public void discoveryResponse() {
        ResponseEntity<OIDCDiscoveryInformationResponse> response = rest.getForEntity(
                URI.create("http://localhost:" + port + "/.well-known/openid-configuration"), OIDCDiscoveryInformationResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void jwksResponse() {
        ResponseEntity<String> response = rest.getForEntity(
                URI.create("http://localhost:" + port + "/oauth2/v3/certs"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void createTokenResponse() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<String, Object>();
        requestParameters.add("realm", "/test");
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "klaus");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid name");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> response = rest.exchange(request, OIDCCreateTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("uid name");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getAccessToken()).isNotEmpty();
    }

    @Test
    public void jwtClaims() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<String, Object>();
        requestParameters.add("realm", "/test");
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "klaus");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid name");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> response = rest.exchange(request, OIDCCreateTokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);


    }
}
