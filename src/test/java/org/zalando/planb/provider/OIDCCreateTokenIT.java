package org.zalando.planb.provider;

import com.datastax.driver.core.Session;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.net.URI;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class OIDCCreateTokenIT extends AbstractSpringTest {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private Session session;

    RestTemplate rest = new RestTemplate();

    @Test
    public void createToken() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<String, Object>();
        requestParameters.add("realm", "/test");
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "klaus");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid name");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString(("foobar" + ':' + "test").getBytes(UTF_8)))
                .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> response = rest.exchange(request, OIDCCreateTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("uid name");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getRealm()).isEqualTo("/test");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());
    }

    @Test
    public void jwtClaims() throws InvalidJwtException, MalformedClaimException {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<String, Object>();
        requestParameters.add("realm", "/test");
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "klaus");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid name");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization",  "Basic " + Base64.getEncoder().encodeToString(("foobar" + ':' + "test").getBytes(UTF_8)))
                .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> response = rest.exchange(request, OIDCCreateTokenResponse.class);
        String jwt = response.getBody().getIdToken();

        // fetch JWK
        HttpsJwks httpsJkws = new HttpsJwks("http://localhost:" + port + "/oauth2/v3/certs");
        HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setVerificationKeyResolver(httpsJwksKeyResolver)
                .build();

        // verify JWT
        JwtContext context = jwtConsumer.process(jwt);
        assertThat(context.getJwtClaims().getSubject()).isEqualTo("klaus");
        assertThat("uid").isIn((Iterable<String>) context.getJwtClaims().getClaimValue("scope"));
        assertThat("name").isIn((Iterable<String>)context.getJwtClaims().getClaimValue("scope"));
        assertThat(context.getJoseObjects().get(0).getKeyIdHeaderValue()).isNotEmpty();
    }

    // TODO 401 on bad client, 401 on bad user, 400 on bad input, 403 on bad scopes in client, 403 on bad scopes in user
    // TODO disable detailed http responses in production mode
}
