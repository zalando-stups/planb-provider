package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Fault;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles({"it"})
public class CustomerUserRealmIT extends AbstractSpringTest {

    public static final String UID = "uid";
    public static final String SUB = "sub";

    public static final String SOAP_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soap:Body>\n" +
                    "        <ns2:authenticateResponse xmlns:ns2=\"http://service.webservice.customer.zalando.de/\">\n" +
                    "            <return>\n" +
                    "                <customerNumber>123456789</customerNumber>\n" +
                    "                <loginResult>SUCCESS</loginResult>\n" +
                    "            </return>\n" +
                    "        </ns2:authenticateResponse>\n" +
                    "    </soap:Body>\n" +
                    "</soap:Envelope>";

    private static final String SOAP_EMPTY_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soap:Body>\n" +
                    "        <ns2:authenticateResponse xmlns:ns2=\"http://service.webservice.customer.zalando.de/\"/>\n" +
                    "    </soap:Body>\n" +
                    "</soap:Envelope>";

    private static final String SOAP_FAILED_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                    "    <soap:Body>\n" +
                    "        <ns2:authenticateResponse xmlns:ns2=\"http://service.webservice.customer.zalando.de/\">\n" +
                    "            <return>\n" +
                    "                <loginResult>FAILED</loginResult>\n" +
                    "            </return>\n" +
                    "        </ns2:authenticateResponse>\n" +
                    "    </soap:Body>\n" +
                    "</soap:Envelope>";

    public static final String WS_CUSTOMER_SERVICE_URL = "/ws/customerService?wsdl";
    public static final String CUSTOMER_NUMBER = "123456789";
    public static final String EMAIL = "user@user.com";
    public static final String PASSWORD = "pwd";

    @Autowired
    private CustomerUserRealm customerUserRealm;

    @Test
    public void testAuthenticate() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo(WS_CUSTOMER_SERVICE_URL))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_RESPONSE)));

        Map<String, String> authenticate = customerUserRealm.authenticate(EMAIL, PASSWORD, newHashSet(UID), emptySet());
        assertThat(authenticate.get(SUB)).isEqualTo(CUSTOMER_NUMBER);
    }

    @Test(expected = RealmAuthenticationException.class)
    public void testNotAuthenticate() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo(WS_CUSTOMER_SERVICE_URL))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_FAILED_RESPONSE)));

        customerUserRealm.authenticate(EMAIL, PASSWORD, newHashSet(UID), emptySet());
    }

    @Test(expected = RealmAuthenticationException.class)
    public void testNotAuthenticateWithEmptyResponse() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo(WS_CUSTOMER_SERVICE_URL))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_EMPTY_RESPONSE)));

        customerUserRealm.authenticate(EMAIL, PASSWORD, newHashSet(UID), emptySet());
    }

    @Test(expected = HystrixRuntimeException.class)
    public void testDependencyUnavailable() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo(WS_CUSTOMER_SERVICE_URL))
                .willReturn(aResponse()
                        .withFault(Fault.EMPTY_RESPONSE)));

        customerUserRealm.authenticate(EMAIL, PASSWORD, newHashSet(UID), emptySet());
    }
}
