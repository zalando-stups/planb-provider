package org.zalando.planb.provider;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.data.MapEntry.entry;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ActiveProfiles("it")
public class OIDCCreateTokenIT extends AbstractOauthTest {

    @Autowired
    private MetricRegistry metricRegistry;

    @Test
    public void createServiceUserToken() {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // NOTE: returned scopes are sorted
        assertThat(response.getBody().getScope()).isEqualTo("ascope uid");
        assertThat(response.getBody().getTokenType()).isEqualTo(OAuth2AccessToken.BEARER_TYPE);
        assertThat(response.getBody().getRealm()).isEqualTo("/services");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getIdToken()).isNull();
    }

    /*
     * Regression test to Issue #115
     */
    @Test
    public void testTokenTypeIsPresentInResponse() {
        ResponseEntity<String> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("token_type");
        assertThat(response.getBody()).contains(OAuth2AccessToken.BEARER_TYPE);
    }

    @Test
    public void createServiceUserTokenWithClientCredentialsInRequestBody() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("realm", "/services");
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        // this is not recommended, but the RFC allows to use request body instead of Authorization header
        requestParameters.add("client_id", "testclient");
        requestParameters.add("client_secret", "test");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAccessTokenUri())
                .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> response = getRestTemplate().exchange(request, OIDCCreateTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTokenType()).isEqualTo(OAuth2AccessToken.BEARER_TYPE);
        assertThat(response.getBody().getAccessToken()).isNotEmpty();
    }

    @Test
    public void createServiceUserTokenUsingWrongHostHeader() throws IOException {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        String basicAuth = Base64.getEncoder().encodeToString(("testclient:test").getBytes(UTF_8));

        // wrong Host header (not mapping to any realm)
        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .header("Host", "token.servicesX.example.org")
                .body(requestParameters);

        try {
            getRestTemplate().exchange(request, OIDCCreateTokenResponse.class);
            fail("Request with invalid Host header should have failed.");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            final Map<String, String> response = getErrorResponseMap(ex);
            assertThat(response).contains(entry("error", "realm_not_found"));
            assertThat(response).contains(entry("error_description", "token.servicesX.example.org not found"));
        }
    }

    @Test
    public void createServiceUserTokenUsingCorrectHostHeader() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        String basicAuth = Base64.getEncoder().encodeToString(("testclient:test").getBytes(UTF_8));

        // Host header contains a valid realm name
        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .header("Host", "token.services.example.org")
                .body(requestParameters);
        ResponseEntity<OIDCCreateTokenResponse> response = getRestTemplate().exchange(request, OIDCCreateTokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRealm()).isEqualTo("/services");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getIdToken()).isNull();
    }

    @Test
    public void createServiceUserTokenUsingDefaultScopes() {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("hello world");
    }

    @Test
    public void createServiceUserTokenExplicitlyRequestingDefaultScopes() {
        // default scopes should be granted even though they have not been configured for this certain user / client
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "hello world uid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("hello uid world");
    }

    @Test
    public void jwtClaims() throws InvalidJwtException, MalformedClaimException {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope");

        String jwt = response.getBody().getAccessToken();

        // fetch JWK
        HttpsJwks httpsJkws = new HttpsJwks(getHttpPublicKeysUri());
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
        final String customerNumber = stubCustomerService();

        final ResponseEntity<OIDCCreateTokenResponse> response = createToken("/customers", "testclient", "test", "testcustomer", "test", "uid openid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).isEqualTo("openid uid"); // scopes are sorted
        assertThat(response.getBody().getTokenType()).isEqualTo(OAuth2AccessToken.BEARER_TYPE);
        assertThat(response.getBody().getRealm()).isEqualTo("/customers");
        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());
        assertThat(response.getBody().getAccessToken().length()).isLessThanOrEqualTo(280);

        JWT jwt = JWTParser.parse(response.getBody().getAccessToken());
        assertThat(jwt.getHeader().toJSONObject()).containsOnlyKeys("kid", "alg");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(customerNumber);
        assertThat(jwt.getJWTClaimsSet().getClaims()).containsOnlyKeys("sub", "realm", "iss", "iat", "exp", "scope");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("realm")).isEqualTo("/customers");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("iss")).isEqualTo("B");
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("scope")).containsExactly("uid", "openid");
    }

    @Test
    public void testCreateCustomerTokenNoScope() throws Exception {
        stubCustomerService();

        final ResponseEntity<OIDCCreateTokenResponse> response = createToken("/customers", "testclient", "test", "testcustomer", "test", null);
        final OIDCCreateTokenResponse body = response.getBody();
        assertThat(body).isNotNull();
        // "openid" scope is set as default for /customers realm (in application-it.yml)
        assertThat(body.getScope()).isEqualTo("openid");
        final String accessToken = body.getAccessToken();
        assertThat(body.getTokenType()).isEqualTo(OAuth2AccessToken.BEARER_TYPE);
        assertThat(body.getRealm()).isEqualTo("/customers");
        assertThat(accessToken).isNotEmpty();
        assertThat(accessToken).isEqualTo(body.getIdToken());
    }

    @Test
    public void testCreateCustomerTokenWithAzpScope() throws Exception {
        final String customerNumber = stubCustomerService();

        final ResponseEntity<OIDCCreateTokenResponse> response = createToken("/customers", "testclient", "test", "testcustomer", "test", "azp");
        assertThat(response.getBody().getScope()).isEqualTo("azp");
        assertThat(response.getBody().getRealm()).isEqualTo("/customers");

        JWT jwt = JWTParser.parse(response.getBody().getAccessToken());
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(customerNumber);
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("scope")).containsExactly("azp");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("azp")).isEqualTo("testclient");
    }

    /**
     * Verify https://github.com/zalando/planb-provider/issues/86
     */
    @Test
    public void createCustomerTokenWithCustomScope() throws Exception {
        final String customerNumber = stubCustomerService();

        final ResponseEntity<OIDCCreateTokenResponse> response = createToken("/customers", "testclient", "test", "testcustomer", "test", "ascope");
        assertThat(response.getBody().getScope()).isEqualTo("ascope");
        assertThat(response.getBody().getRealm()).isEqualTo("/customers");

        JWT jwt = JWTParser.parse(response.getBody().getAccessToken());
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(customerNumber);
        assertThat(jwt.getJWTClaimsSet().getStringListClaim("scope")).containsExactly("ascope");
    }

    /**
     * Verify https://github.com/zalando/planb-provider/issues/86
     */
    @Test
    public void createCustomerTokenWithScopeForbiddenByClient() throws Exception {
        stubCustomerService();

        try {
            createToken("/customers", "testclient", "test", "testcustomer", "test", "invalidscope");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_scope"));
        }
    }

    @Test
    public void unknownRealm() throws IOException {
        try {
            createToken("/wrong", "testclient", "test", "testuser", "wrong", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "realm_not_found"));
            assertThat(metricRegistry.getTimers()).containsKey("planb.provider.access_token.unknown-realm.error.other");
        }
    }

    @Test
    public void unauthenticatedClient() throws IOException {
        try {
            createToken("/services", "testclient", "wrong", "testuser", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(UNAUTHORIZED);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_client"));
        }
    }

    @Test
    public void unauthenticatedUser() throws IOException {
        try {
            createToken("/services", "testclient", "test", "testuser", "wrong", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_grant"));
        }
    }

    @Test
    public void unknownClient() throws IOException {
        try {
            createToken("/services", "wrong", "test", "testuser", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(UNAUTHORIZED);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_client"));
        }
    }

    @Test
    public void unknownUser() throws IOException {
        try {
            createToken("/services", "testclient", "test", "wrong", "test", "uid ascope");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_grant"));
        }
    }

    @Test
    public void unauthorizedClientScopes() throws IOException {
        try {
            createToken("/services", "testclient", "test", "testuser", "test", "useronly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_scope"));
        }
    }

    @Test
    public void unauthorizedUserScopes() throws Exception {
        try {
            createToken("/services", "testclient", "test", "testuser", "test", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_scope"));
        }
    }

    @Test
    public void emptyUsername() throws Exception {
        try {
            createToken("/services", "testclient", "test", " ", "test", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_grant"));
        }
    }

    @Test
    public void emptyPassword() throws Exception {
        try {
            createToken("/services", "testclient", "test", "testuser", " ", "clientonly");
            fail("request should have failed");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST);
            assertThat(getErrorResponseMap(e)).contains(entry("error", "invalid_grant"));
        }
    }

    @Test
    public void testPublicClient() throws Exception {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services", "testpublicclient", "whatever", "testuser", "test", "uid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static Map<String, String> getErrorResponseMap(HttpStatusCodeException e) throws IOException {
        return new ObjectMapper().readValue(e.getResponseBodyAsByteArray(), new TypeReference<Map<String, String>>() {
        });
    }
}
