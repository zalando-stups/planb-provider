package org.zalando.planb.provider;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.zalando.planb.provider.AuthorizationCodeGrantFlowIT.parseURLParams;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class ImplicitGrantFlowIT extends AbstractSpringTest {
    @Value("${local.server.port}")
    private int port;


    private final HttpClient httpClient = HttpClients.custom().disableRedirectHandling().build();

    private final RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    @Test
    public void showLoginForm() {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create("http://localhost:" + port + "/oauth2/authorize?realm=/services&response_type=token&client_id=testimplicit&redirect_uri=https://myapp.example.org/callback&state=mystate"))
                .build();

        ResponseEntity<String> response = rest.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("<form");
        assertThat(response.getBody()).contains("value=\"mystate\"");
    }

    @Test
    public void confidentialClientNotAllowed() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .body(requestParameters);

        try {
            rest.exchange(request, Void.class);
            fail("Implicit Grant flow should only be allowed for non-confidential clients");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid response_type 'token' for confidential client");
        }
    }

    @Test
    public void authorizeSuccess() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testimplicit");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .body(requestParameters);

        ResponseEntity<String> loginResponse = rest.exchange(request, String.class);
        assertThat(loginResponse.getBody()).contains("value=\"allow\"");

        requestParameters.add("decision", "allow");

        ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?");
        Map<String, String> params = parseURLParams(authResponse.getHeaders().getLocation());
        // http://tools.ietf.org/html/rfc6749#section-4.2.2
        // check required parameters
        assertThat(params).containsKey("access_token");
        assertThat(params).contains(MapEntry.entry("token_type", "Bearer"));
        assertThat(params).contains(MapEntry.entry("expires_in", "28800")); // 8 hours
        assertThat(params).containsKey("scope");
        assertThat(params).containsKey("state");
    }
}
