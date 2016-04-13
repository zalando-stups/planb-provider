package org.zalando.planb.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.post;

@ActiveProfiles("it")
public class AuthorizationCodeGrantFlowIT extends AbstractOauthTest {

    private final ObjectMapper om = new ObjectMapper();

    @Autowired
    private ConsentService consentService;

    @Test
    public void showLoginForm() {
        RequestEntity<Void> request = RequestEntity
                .get(getAuthorizeUrl("code", "/services", "testauthcode", "https://myapp.example.org/callback", "mystate"))
                .build();

        ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains("<form");
        assertThat(response.getBody()).contains("value=\"mystate\"");
    }

    @Test
    public void invalidResponseType() {
        RequestEntity<Void> request = RequestEntity
                .get(getAuthorizeUrl("foo", "/services", "testauthcode", "https://myapp.example.org/callback", ""))
                .build();

        try {
            getRestTemplate().exchange(request, String.class);
            fail("GET should have thrown Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("response_type");
        }
    }

    @Test
    public void redirectUriMissing() {
        RequestEntity<Void> request = RequestEntity
                .get(getAuthorizeUrl("code", "/services", "testclient", "", ""))
                .build();

        try {
            getRestTemplate().exchange(request, String.class);
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

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .body(requestParameters);

        try {
            getRestTemplate().exchange(request, Void.class);
            fail("POST should have thrown Bad Request");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Redirect URI mismatch");
        }

    }

    @Test
    public void authorizeWrongRedirectUri() {
        // assume prior user consent
        consentService.store("testuser", "/services", "testauthcode", ImmutableSet.of("uid", "ascope"));

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);

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

        RequestEntity<MultiValueMap<String, Object>> request2 =
                post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        try {
            getRestTemplate().exchange(request2, OIDCCreateTokenResponse.class);
            fail("Token creation should have failed with 'client mismatch'");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid authorization code: redirect_uri mismatch");
        }
    }

    @Test
    public void authorizeWrongClient() {
        // assume prior user consent
        consentService.store("testuser", "/services", "testauthcode", ImmutableSet.of("uid", "ascope"));

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);

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

        RequestEntity<MultiValueMap<String, Object>> request2 =
                post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        try {
            getRestTemplate().exchange(request2, OIDCCreateTokenResponse.class);
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
    public void authorizeWrongUserCredentialsAsJson() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "wrongpass");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        try {
            getRestTemplate().exchange(request, Void.class);
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("invalid_grant");
        }

    }

    @Test
    public void authorizeWrongUserCredentialsAsHTML() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "wrongpass");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);

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
    public void testAuthorizeConsentDenied() {
        final MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        final ResponseEntity<String> loginResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.TEXT_HTML).body(requestParameters),
                String.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(OK);
        assertThat(loginResponse.getBody()).contains("<h1>Consent</h1>");

        requestParameters.add("decision", "deny");

        final ResponseEntity<Void> authResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.TEXT_HTML).body(requestParameters),
                Void.class);


        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        final URI location = authResponse.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).startsWith("https://myapp.example.org/callback");
        final List<NameValuePair> params = URLEncodedUtils.parse(location, UTF_8.name());
        assertThat(params).extracting("name", String.class).doesNotContain("code");
        assertThat(params).extracting("name", "value").contains(tuple("error", "access_denied"));
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

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<String> loginResponse = getRestTemplate().exchange(request, String.class);
        assertThat(loginResponse.getBody()).contains("value=\"allow\"");

        requestParameters.add("decision", "allow");

        request =
                post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);


        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?code=");
        List<NameValuePair> params = URLEncodedUtils.parse(authResponse.getHeaders().getLocation(), "UTF-8");
        String code = params.stream().filter(p -> p.getName().equals("code")).findFirst().get().getValue();

        MultiValueMap<String, Object> requestParameters2 = new LinkedMultiValueMap<>();
        requestParameters2.add("grant_type", "authorization_code");
        requestParameters2.add("code", code);
        requestParameters2.add("redirect_uri", requestParameters.getFirst("redirect_uri"));
        String basicAuth = Base64.getEncoder().encodeToString(("testauthcode" + ':' + "test").getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request2 =
                post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters2);

        ResponseEntity<OIDCCreateTokenResponse> response = getRestTemplate().exchange(request2, OIDCCreateTokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().getScope()).contains("uid");
        assertThat(response.getBody().getScope()).contains("ascope");
        assertThat(response.getBody().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getRealm()).isEqualTo("/services");

        assertThat(response.getBody().getAccessToken()).isNotEmpty();
        assertThat(response.getBody().getAccessToken()).isEqualTo(response.getBody().getIdToken());

        // test that we cannot use the same code twice
        try {
            getRestTemplate().exchange(request2, OIDCCreateTokenResponse.class);
            fail("Authorization code should have been invalidated");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("Invalid authorization code");
        }

    }

    @Test
    public void testRenderSimpleConsentHtml() throws Exception {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testconsentsimple");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        final ResponseEntity<String> loginResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.TEXT_HTML).body(requestParameters),
                String.class);

        assertThat(loginResponse.getBody())
                .isXmlEqualToContentOf(new ClassPathResource("/golden-files/consent-authcode-simple.html").getFile());
    }

    @Test
    public void testRenderSimpleConsentJson() throws Exception {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testconsentsimple");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        final ResponseEntity<String> loginResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.APPLICATION_JSON).body(requestParameters),
                String.class);
        assertThat(om.readTree(loginResponse.getBody()))
                .isEqualTo(om.readTree(new ClassPathResource("/golden-files/consent-simple.json").getInputStream()));
    }

    @Test
    public void testRenderConsentWithMetaDataHtml() throws Exception {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testconsentwithmetadata");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        final ResponseEntity<String> loginResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.TEXT_HTML).body(requestParameters),
                String.class);

        assertThat(loginResponse.getBody())
                .isXmlEqualToContentOf(new ClassPathResource("/golden-files/consent-authcode-with-meta-data.html").getFile());
    }

    @Test
    public void testRenderConsentWithMetaDataJson() throws Exception {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testconsentwithmetadata");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope openid");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        final ResponseEntity<String> loginResponse = getRestTemplate().exchange(
                post(getAuthorizeUrl()).accept(MediaType.APPLICATION_JSON).body(requestParameters),
                String.class);

        assertThat(om.readTree(loginResponse.getBody()))
                .isEqualTo(om.readTree(new ClassPathResource("/golden-files/consent-with-meta-data.json").getInputStream()));
    }
}
