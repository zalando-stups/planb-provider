package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
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
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.fail;
import static org.assertj.core.api.StrictAssertions.failBecauseExceptionWasNotThrown;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class OIDCCreateTokenIT extends AbstractSpringTest {
    @Value("${local.server.port}")
    private int port;

    private final RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    private ResponseEntity<OIDCCreateTokenResponse> createToken(String realm, String clientId, String clientSecret,
                                                                String username, String password, String scope) {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("realm", realm);
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", username);
        requestParameters.add("password", password);
        requestParameters.add("scope", scope);
        String basicAuth = Base64.getEncoder().encodeToString((clientId + ':' + clientSecret).getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters);

        return rest.exchange(request, OIDCCreateTokenResponse.class);
    }

    @Test
    public void createServiceUserToken() {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("uid ascope");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getRealm()).isEqualTo("/services");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());
    }

    @Test
    public void jwtClaims() throws InvalidJwtException, MalformedClaimException {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope");

        String jwt = response.getBody().getIdToken();

        // fetch JWK
        HttpsJwks httpsJkws = new HttpsJwks("http://localhost:" + port + "/oauth2/connect/keys");
        HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJkws);
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setVerificationKeyResolver(httpsJwksKeyResolver)
                .build();

        // verify JWT
        JwtContext context = jwtConsumer.process(jwt);

        // proper subject
        assertThat(context.getJwtClaims().getSubject()).isEqualTo("testuser");

        // custom claims
        assertThat("uid").isIn((Iterable<?>) context.getJwtClaims().getClaimValue("scope"));
        assertThat("ascope").isIn((Iterable<?>) context.getJwtClaims().getClaimValue("scope"));
        assertThat("/services").isEqualTo(context.getJwtClaims().getClaimValue("realm"));

        // kid set in header for precise key matching
        assertThat(context.getJoseObjects().get(0).getKeyIdHeaderValue()).isNotEmpty();
    }

    @Test
    public void testCreateCustomerToken() throws Exception {
        stubFor(post(urlEqualTo("/ws/customerService?wsdl"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                "    <soap:Body>\n" +
                                "        <ns2:authenticateResponse xmlns:ns2=\"http://service.webservice.customer.zalando.de/\">\n" +
                                "            <return>\n" +
                                "                <customerNumber>123456789</customerNumber>\n" +
                                "                <loginResult>SUCCESS</loginResult>\n" +
                                "            </return>\n" +
                                "        </ns2:authenticateResponse>\n" +
                                "    </soap:Body>\n" +
                                "</soap:Envelope>")));

        final ResponseEntity<OIDCCreateTokenResponse> response = createToken("/customers", "testclient", "test", "testcustomer", "test", "uid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("uid");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getRealm()).isEqualTo("/customers");
        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());
    }

    @Test
    public void unknownRealm() {
        try {
            createToken("/wrong", "testclient", "test", "testuser", "wrong", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unauthenticatedClient() {
        try {
            createToken("/services", "testclient", "wrong", "testuser", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unauthenticatedUser() {
        try {
            createToken("/services", "testclient", "test", "testuser", "wrong", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unknownClient() {
        try {
            createToken("/services", "wrong", "test", "testuser", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unknownUser() {
        try {
            createToken("/services", "testclient", "test", "wrong", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unauthorizedClientScopes() {
        try {
            createToken("/services", "testclient", "test", "testuser", "test", "useronly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void unauthorizedUserScopes() {
        try {
            createToken("/services", "testclient", "test", "testuser", "test", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void emptyUsername() {
        try {
            createToken("/services", "testclient", "test", " ", "test", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void emptyPassword() {
        try {
            createToken("/services", "testclient", "test", "testuser", " ", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }

    @Test
    public void testPublicClient() throws Exception {
        try {
            createToken("/services", "testpublicclient", "test", "testuser", "test", "uid");
            failBecauseExceptionWasNotThrown(HttpClientErrorException.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            e.getResponseBodyAsString(); // for preventing broken pipe loggings for now
        }
    }
}
