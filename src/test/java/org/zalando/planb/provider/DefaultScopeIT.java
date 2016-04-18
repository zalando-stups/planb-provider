package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.RequestEntity.post;

@ActiveProfiles("it")
public class DefaultScopeIT extends AbstractOauthTest {

    @Test
    public void clientSpecificDefaultsImplicitGrantTest() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testdefaultscope");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getScopes()).containsExactly("uid");
    }

    @Test
    public void clientSpecificDefaultsResourceOwnerPasswordGrantTest() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("grant_type", "password");
        requestParameters.add("realm", "/services");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");

        String basicAuth = Base64.getEncoder().encodeToString(("testdefaultscope" + ':' + "test").getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request =
                post(getAccessTokenUri())
                        .header("Authorization", "Basic " + basicAuth)
                        .body(requestParameters);

        ResponseEntity<OIDCCreateTokenResponse> loginResponse = getRestTemplate().exchange(request, OIDCCreateTokenResponse.class);
        assertThat(loginResponse.getBody().getRealm()).isEqualTo("/services");
        assertThat(loginResponse.getBody().getScope()).isEqualTo("uid");
    }

    @Test
    public void fallbackToRealmWideDefaultsImplicitGrantScope() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "token");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testimplicit");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getScopes()).containsExactly("world", "hello");
    }

    @Test
    public void clientSpecificDefaultsAuthorizationCodeGrantTest() {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testdefaultscope");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getScopes()).containsExactly("uid");
    }

    @Test
    public void fallbackToRealmWideDefaultsAuthorizationCodeGrantTest() {

        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("response_type", "code");
        requestParameters.add("realm", "/services");
        requestParameters.add("client_id", "testauthcode");
        requestParameters.add("username", "testuser");
        requestParameters.add("password", "test");
        requestParameters.add("redirect_uri", "https://myapp.example.org/callback");

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAuthorizeUrl())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestParameters);

        ResponseEntity<AuthorizeResponse> loginResponse = getRestTemplate().exchange(request, AuthorizeResponse.class);
        assertThat(loginResponse.getBody().getScopes()).containsExactly("world", "hello");
    }

}
