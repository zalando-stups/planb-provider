package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles({"it"})
public class CustomerUserRealmIT extends AbstractSpringTest {

    private static final Logger log = LoggerFactory.getLogger(CustomerUserRealmIT.class);

    public static final String UID = "uid";

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

    @Autowired
    private CustomerUserRealm customerUserRealm;

    @Test
    public void testAuthenticate() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo("/ws/customerService?wsdl"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_RESPONSE)));

        Map<String, Object> authenticate = customerUserRealm.authenticate("user@user.com", "pwd", new String[]{UID});
        assertThat(authenticate.get(UID)).isEqualTo("123456789");
    }

    @Test(expected = RealmAuthenticationException.class)
    public void testNotAuthenticate() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo("/ws/customerService?wsdl"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_FAILED_RESPONSE)));

        Map<String, Object> authenticate = customerUserRealm.authenticate("user@user.com", "pwd", new String[]{UID});
    }

    @Test(expected = RealmAuthenticationException.class)
    public void testNotAuthenticateWithEmptyResponse() throws RealmAuthenticationException {

        stubFor(post(urlEqualTo("/ws/customerService?wsdl"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, TEXT_XML_VALUE)
                        .withBody(SOAP_EMPTY_RESPONSE)));

        Map<String, Object> authenticate = customerUserRealm.authenticate("user@user.com", "pwd", new String[]{UID});
    }
}