package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.zalando.planb.provider.api.Client;
import org.zalando.planb.provider.api.User;

import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.RequestEntity.post;
import static org.springframework.http.RequestEntity.put;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class ServiceUserAuthenticationIT extends AbstractSpringTest {

    @Value("${local.server.port}")
    private int port;

    private final RestOperations http = new RestTemplate();

    @Test
    public void testServiceUserFlow() throws Exception {
        final String clientId = "test-service_0815";
        final String clientSecret = "cL!3Nt";
        final String userPassword = "p455W0rD";
        final String username = "test-service";
        final String scope = "uid";
        final String realm = "/services";

        // Create the client
        final Client client = new Client();
        client.setIsConfidential(true);
        client.setSecretHash(hashAndEncodePassword(clientSecret));
        client.setScopes(singletonList(scope));
        http.exchange(put(URI.create("http://localhost:" + port + "/clients" + realm + "/" + clientId))
                .contentType(APPLICATION_JSON)
                .body(client), Void.class);

        // Create the user
        final User user = new User();
        user.setPasswordHashes(singletonList(hashAndEncodePassword(userPassword)));
        user.setScopes(singletonMap(scope, "test-service"));
        http.exchange(put(URI.create("http://localhost:" + port + "/users" + realm + "/" + username))
                .contentType(APPLICATION_JSON)
                .body(user), Void.class);

        // Get an access token for the newly created user
        final MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<String, Object>();
        requestParameters.add("realm", realm);
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", username);
        requestParameters.add("password", userPassword);
        requestParameters.add("scope", scope);

        final ResponseEntity<OIDCCreateTokenResponse> response = http.exchange(
                post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                        .header("Authorization", Base64.getEncoder().encodeToString((clientId + ':' + clientSecret).getBytes(UTF_8)))
                        .body(requestParameters),
                OIDCCreateTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final OIDCCreateTokenResponse tokenResponse = response.getBody();
        assertThat(tokenResponse.getScope()).isEqualTo("uid");
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getRealm()).isEqualTo("/services");
        assertThat(tokenResponse.getAccessToken()).isNotEmpty();
        assertThat(tokenResponse.getAccessToken()).isEqualTo(tokenResponse.getIdToken());

        System.out.println(tokenResponse.getAccessToken());
    }

    private String hashAndEncodePassword(String clientSecret) {
        final String pwHash = BCrypt.hashpw(clientSecret, BCrypt.gensalt());
        return Base64.getEncoder().encodeToString(pwHash.getBytes(UTF_8));
    }
}
