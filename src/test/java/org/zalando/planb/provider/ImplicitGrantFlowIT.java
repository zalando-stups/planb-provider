package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClients;
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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.bouncycastle.asn1.ua.DSTU4145NamedCurves.params;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;
import static org.springframework.security.oauth2.common.AuthenticationScheme.query;
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

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback#");
        Map<String, String> params = parseURLFragments(authResponse.getHeaders().getLocation());
        // http://tools.ietf.org/html/rfc6749#section-4.2.2
        // check required parameters
        assertThat(params).containsKey("access_token");
        assertThat(params).contains(MapEntry.entry("token_type", "Bearer"));
        assertThat(params).contains(MapEntry.entry("expires_in", "3600"));
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

        assertThat(authResponse.getBody().getRedirect()).startsWith("https://myapp.example.org/callback#");

        Map<String, String> params = parseURLFragments(URI.create(authResponse.getBody().getRedirect()));
        // http://tools.ietf.org/html/rfc6749#section-4.2.2
        // check required parameters
        assertThat(params).containsKey("access_token");
        assertThat(params).contains(MapEntry.entry("token_type", "Bearer"));
        assertThat(params).contains(MapEntry.entry("expires_in", "3600"));
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

        assertThat(authResponse.getHeaders().getLocation().toString()).startsWith("https://myapp.example.org/callback#");
        Map<String, String> params = parseURLFragments(authResponse.getHeaders().getLocation());
        assertThat(params).contains(MapEntry.entry("error", "access_denied"));
        assertThat(params).containsKey("state");
    }

    /**
     * Verify https://github.com/zalando/planb-provider/issues/86
     */
    @Test
    public void customerScopeAllowedByClient() {
        stubCustomerService();

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/customers");
        requestParameters.add("client_id", "testcustomerimplicit");
        requestParameters.add("username", "test@example.org");
        requestParameters.add("password", "mypass");
        requestParameters.add("scope", "uid read-customer-profile");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + getPort() + "/oauth2/authorize"))
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getClientName()).isEqualTo("Test Customer App");
        assertThat(loginResponse.getBody().getScopes()).containsExactly("uid", "read-customer-profile");
    }

    @Test
    public void customerScopeForbiddenByClient() {
        stubCustomerService();

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/customers");
        requestParameters.add("client_id", "testcustomerimplicit");
        requestParameters.add("username", "test@example.org");
        requestParameters.add("password", "mypass");
        requestParameters.add("scope", "uid ascope invalidscope");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(URI.create("http://localhost:" + getPort() + "/oauth2/authorize"))
                .body(requestParameters);

        try {
            getRestTemplate().exchange(request, Void.class);
            fail("Implicit Grant flow should restrict scopes by client");
        } catch (HttpClientErrorException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getResponseBodyAsString()).contains("invalid_scope");
        }
    }

    static Map<String, String> parseURLFragments(URI uri) {
        List<NameValuePair> nameValuePairs = parse(uri, "UTF-8");

        return nameValuePairs.stream()
                .collect(groupingBy(NameValuePair::getName, reducing("", NameValuePair::getValue, (x, y) -> y)));
    }

    static List <NameValuePair> parse(final URI uri, final String charset) {
        final String query = uri.getRawFragment();
        if (query != null && !query.isEmpty()) {
            return URLEncodedUtils.parse(query, Charset.forName(charset));
        }
        return Collections.emptyList();
    }
}
