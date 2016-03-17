package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClients;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.offset;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class AuthorizationCodeGrantFlowIT extends AbstractSpringTest {
    @Value("${local.server.port}")
    private int port;

    @Autowired
    CassandraConsentService cassandraConsentService;


    private final HttpClient httpClient = HttpClients.custom().disableRedirectHandling().build();

    private final RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    @Test
    public void showLoginForm() {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create("http://localhost:" + port + "/oauth2/authorize?realm=/services&response_type=code&client_id=testauthcode&redirect_uri=https://myapp.example.org/callback&state=mystate"))
                .build();

        ResponseEntity<String> response = rest.exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("<form");
        assertThat(response.getBody()).contains("value=\"mystate\"");
    }

    @Test
    public void invalidResponseType() {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create("http://localhost:" + port + "/oauth2/authorize?realm=/services&response_type=foo&client_id=testauthcode"))
                .build();

        try {
            ResponseEntity<String> response = rest.exchange(request, String.class);
            fail("GET should have thrown Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("response_type");
        }
    }

    @Test
    public void redirectUriMissing() {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create("http://localhost:" + port + "/oauth2/authorize?realm=/services&response_type=code&client_id=testclient"))
                .build();

        try {
            ResponseEntity<String> response = rest.exchange(request, String.class);
            fail("GET should have thrown Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Missing redirect_uri");
        }
    }

    @Test
    public void redirectUriMismatch() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://wrong.redirect.uri.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .body(requestParameters);

        try {
            ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);
            fail("POST should have thrown Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Redirect URI mismatch");
        }

    }

    @Test
    public void authorizeWrongRedirectUri() {
        // assume prior user consent
        cassandraConsentService.store("testuser", "/services", "testauthcode", ImmutableSet.of("uid", "ascope"));

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?code=");
        List<NameValuePair> params = URLEncodedUtils.parse(authResponse.getHeaders().getLocation(), "UTF-8");
        String code = params.stream().filter(p -> p.getName().equals("code")).findFirst().get().getValue();

        MultiValueMap<String, Object> requestParameters2 = new LinkedMultiValueMap<>();
        requestParameters2.add("grant_type", "authorization_code");
        requestParameters2.add("code", code);
        requestParameters2.add("redirect_uri", "https://evil.site.example.org");
        // NOTE: we are using valid client credentials, but it's the wrong one (we used testauthcode above!)
        String basicAuth = Base64.getEncoder().encodeToString(("testclient" + ':' + "test").getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request2 = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        try {
            rest.exchange(request2, OIDCCreateTokenResponse.class);
            fail("Token creation should have failed with 'client mismatch'");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid authorization code: redirect_uri mismatch");
        }
    }

    @Test
    public void authorizeWrongClient() {
        // assume prior user consent
        cassandraConsentService.store("testuser", "/services", "testauthcode", ImmutableSet.of("uid", "ascope"));

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?code=");
        List<NameValuePair> params = URLEncodedUtils.parse(authResponse.getHeaders().getLocation(), "UTF-8");
        String code = params.stream().filter(p -> p.getName().equals("code")).findFirst().get().getValue();

        MultiValueMap<String, Object> requestParameters2 = new LinkedMultiValueMap<>();
        requestParameters2.add("grant_type", "authorization_code");
        requestParameters2.add("code", code);
        requestParameters2.add("redirect_uri", requestParameters.getFirst("redirect_uri"));
        // NOTE: we are using valid client credentials, but it's the wrong one (we used testauthcode above!)
        String basicAuth = Base64.getEncoder().encodeToString(("testclient" + ':' + "test").getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request2 = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        try {
            rest.exchange(request2, OIDCCreateTokenResponse.class);
            fail("Token creation should have failed with 'client mismatch'");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid authorization code: client mismatch");
        }
    }

    static Map<String, String> parseURLParams(URI uri) {

        List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(uri, "UTF-8");

        return nameValuePairs.stream()
                .collect(groupingBy(NameValuePair::getName, reducing("", NameValuePair::getValue, (x, y) -> y)));
    }


    @Test
    public void authorizeWrongCredentials() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "wrongpass");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).contains("/oauth2/authorize");
        Map<String, String> params = parseURLParams(authResponse.getHeaders().getLocation());
        assertThat(params).contains(MapEntry.entry("error", "access_denied"));
        // ensure that all original params are preserved when redirecting to the error page
        assertThat(params).contains(MapEntry.entry("response_type", "code"));
        assertThat(params).contains(MapEntry.entry("realm", "/services"));
        assertThat(params).contains(MapEntry.entry("client_id", "testauthcode"));
        assertThat(params).contains(MapEntry.entry("scope", "ascope openid uid")); // is sorted
        assertThat(params).contains(MapEntry.entry("redirect_uri", "https://myapp.example.org/callback"));

    }

    @Test
    public void authorizeSuccess() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<String> loginResponse = rest.exchange(request, String.class);
        assertThat(loginResponse.getBody()).contains("value=\"allow\"");

        requestParameters.add("decision", "allow");

        request = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/authorize"))
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = rest.exchange(request, Void.class);


        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?code=");
        List<NameValuePair> params = URLEncodedUtils.parse(authResponse.getHeaders().getLocation(), "UTF-8");
        String code = params.stream().filter(p -> p.getName().equals("code")).findFirst().get().getValue();

        MultiValueMap<String, Object> requestParameters2 = new LinkedMultiValueMap<>();
        requestParameters2.add("grant_type", "authorization_code");
        requestParameters2.add("code", code);
        requestParameters2.add("redirect_uri", requestParameters.getFirst("redirect_uri"));
        String basicAuth = Base64.getEncoder().encodeToString(("testauthcode" + ':' + "test").getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request2 = RequestEntity
                .post(URI.create("http://localhost:" + port + "/oauth2/access_token"))
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        ResponseEntity<OIDCCreateTokenResponse> response = rest.exchange(request2, OIDCCreateTokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getScope()).contains("uid");
        assertThat(response.getBody().getScope()).contains("ascope");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getRealm()).isEqualTo("/services");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());

        // test that we cannot use the same code twice
        try {
            rest.exchange(request2, OIDCCreateTokenResponse.class);
            fail("Authorization code should have been invalidated");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid authorization code");
        }

    }
}
