package org.zalando.planb.provider;

import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.zalando.planb.provider.AuthorizationCodeGrantFlowIT.parseURLParams;

@ActiveProfiles("it")
public class ImplicitGrantFlowIT extends AbstractOauthTest {

    @Autowired
    private CassandraConsentService cassandraConsentService;

    @Test
    public void showLoginForm() throws URISyntaxException {
        RequestEntity<Void> request = RequestEntity
                .get(getAuthorizeUrl("token", "/services", "testimplicit", "https://myapp.example.org/callback", "mystate"))
                .build();

        ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);

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
                .post(getAuthorizeUrl())
                .body(requestParameters);

        try {
            getRestTemplate().exchange(request, Void.class);
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
                .post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<String> loginResponse = getRestTemplate().exchange(request, String.class);
        assertThat(loginResponse.getBody()).contains("value=\"allow\"");

        requestParameters.add("decision", "allow");

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);

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

    @Test
    public void authorizeConsentAsJson() {
        cassandraConsentService.withdraw("testuser", "/services", "testimplicit");
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testimplicit");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getScopes()).containsExactly("uid", "ascope");

        requestParameters.add("decision", "allow");

        ResponseEntity<AuthorizeResponse> authResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(authResponse.getBody().getRedirect()).startsWith("https://myapp.example.org/callback?");
        Map<String, String> params = parseURLParams(URI.create(authResponse.getBody().getRedirect()));
        // http://tools.ietf.org/html/rfc6749#section-4.2.2
        // check required parameters
        assertThat(params).containsKey("access_token");
        assertThat(params).contains(MapEntry.entry("token_type", "Bearer"));
        assertThat(params).contains(MapEntry.entry("expires_in", "28800")); // 8 hours
        assertThat(params).containsKey("scope");
        assertThat(params).containsKey("state");
    }

    @Test
    public void denyConsent() {
        cassandraConsentService.withdraw("testuser", "/services", "testimplicit");
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testimplicit");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("scope", "uid ascope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.TEXT_HTML)
                .body(requestParameters);

        ResponseEntity<String> loginResponse = getRestTemplate().exchange(request, String.class);
        assertThat(loginResponse.getBody()).contains("value=\"deny\"");

        requestParameters.add("decision", "deny");

        ResponseEntity<Void> authResponse = getRestTemplate().exchange(request, Void.class);

        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.FOUND);

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback?");
        Map<String, String> params = parseURLParams(authResponse.getHeaders().getLocation());
        assertThat(params).contains(MapEntry.entry("error", "access_denied"));
        assertThat(params).containsKey("state");
    }
}
