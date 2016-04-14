package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import exclude.from.component.scan.CassandraTestAddressTranslatorConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

@SpringApplicationConfiguration(classes = {Main.class, CassandraTestAddressTranslatorConfig.class})
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

    protected ResponseEntity<OIDCCreateTokenResponse> createToken(String realm, String clientId, String clientSecret,
                                                                String username, String password, String scope) {
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("realm", realm);
        requestParameters.add("grant_type", "password");
        requestParameters.add("username", username);
        requestParameters.add("password", password);
        if (scope != null) {
            requestParameters.add("scope", scope);
        }
        String basicAuth = Base64.getEncoder().encodeToString((clientId + ':' + clientSecret).getBytes(UTF_8));

        RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
                .post(getAccessTokenUri())
                .header("Authorization", "Basic " + basicAuth)
                .body(requestParameters);

        return getRestTemplate().exchange(request, OIDCCreateTokenResponse.class);
    }

    protected String stubCustomerService() {
        final String customerNumber = "123456789";
        stubFor(post(urlEqualTo("/ws/customerService?wsdl"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                "    <soap:Body>\n" +
                                "        <ns2:authenticateResponse xmlns:ns2=\"http://service.webservice.customer.zalando.de/\">\n" +
                                "            <return>\n" +
                                "                <customerNumber>" + customerNumber + "</customerNumber>\n" +
                                "                <loginResult>SUCCESS</loginResult>\n" +
                                "            </return>\n" +
                                "        </ns2:authenticateResponse>\n" +
                                "    </soap:Body>\n" +
                                "</soap:Envelope>")));
        return customerNumber;
    }
}
