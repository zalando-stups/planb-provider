package org.zalando.planb.provider;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
public class AbstractOauthTest extends AbstractSpringTest {

    private static final String HTTP_LOCALHOST = "http://localhost:";
    private static final String HTTPS_LOCALHOST = "https://localhost:";

    private final HttpClient httpClient = HttpClients.custom().disableRedirectHandling().build();

    private final RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    @Value("${local.server.port}")
    private int port;

    protected int getPort() {
        return port;
    }

    protected RestTemplate getRestTemplate() {
        return rest;
    }

    protected URI getAuthorizeUrl(String responseType, String realm, String clientId, String redirectURI, String state) {
        try {
            final URIBuilder builder = getAuthorizeUrlBuilder();
            if (StringUtils.isNotEmpty(responseType)) {
                builder.addParameter("response_type", responseType);
            }
            if (StringUtils.isNotEmpty(realm)) {
                builder.addParameter("realm", realm);
            }
            if (StringUtils.isNotEmpty(clientId)) {
                builder.addParameter("client_id", clientId);
            }
            if (StringUtils.isNotEmpty(redirectURI)) {
                builder.addParameter("redirect_uri", redirectURI);
            }
            if (StringUtils.isNotEmpty(state)) {
                builder.addParameter("state", state);
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected URIBuilder getAuthorizeUrlBuilder() {
        return new URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPort(getPort())
                .setPath("/oauth2/authorize");

    }

    protected URI getAccessTokenUri() {
        return URI.create(getUriWithPathAsString("/oauth2/access_token"));
    }

    protected URI getAuthorizeUrl() {
        try {
            return getAuthorizeUrlBuilder().build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getRawSyncBaseUri() {
        return getUriWithPathAsString("/raw-sync");
    }

    protected String getConsentsBaseUri() {
        return getUriWithPathAsString("/consents");
    }

    protected String getHttpPublicKeysUri(){
        return getUriWithPathAsString("/oauth2/connect/keys");
    }
    protected String getHttpsPublicKeysUri(){
        return HTTPS_LOCALHOST + getPort()+ "/oauth2/connect/keys";
    }

    protected String getOpenIdConfiguration() {
        return getUriWithPathAsString("/.well-known/openid-configuration");
    }

    protected URI getUriWithPathAsUri(String path) {
        return URI.create(getUriWithPathAsString(path));
    }

    protected String getUriWithPathAsString(String path) {
        return HTTP_LOCALHOST + getPort()+ path;
    }
}
