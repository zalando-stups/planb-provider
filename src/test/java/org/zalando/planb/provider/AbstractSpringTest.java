package org.zalando.planb.provider;

import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public abstract class AbstractSpringTest {

    protected static final String USER1_ACCESS_TOKEN = "Bearer 987651111";
    protected static final String USER2_ACCESS_TOKEN = "Bearer 987652222";
    protected static final String INVALID_ACCESS_TOKEN = "Bearer 123456789";
    protected static final String INSUFFICIENT_SCOPES_ACCESS_TOKEN = "Bearer 111222333";

    protected static final String USER1 = "/services/user1";
    protected static final String USER2 = "/employees/user2";

    private static final String USER1_TOKENINFO_RESPONSE = "{\n" +
            "    \"uid\": \"user1\",\n" +
            "    \"scope\": [\n" +
            "        \"uid\",\n" +
            "        \"hello\"\n" +
            "    ],\n" +
            "    \"hello\": \"World\",\n" +
            "    \"expires_in\": 99999,\n" +
            "    \"token_type\": \"Bearer\",\n" +
            "    \"access_token\": \"987654321\",\n" +
            "    \"realm\": \"/services\"\n" +
            "}";

    private static final String USER2_TOKENINFO_RESPONSE = "{\n" +
            "    \"uid\": \"user2\",\n" +
            "    \"scope\": [\n" +
            "        \"uid\",\n" +
            "        \"hello\"\n" +
            "    ],\n" +
            "    \"hello\": \"World\",\n" +
            "    \"expires_in\": 99999,\n" +
            "    \"token_type\": \"Bearer\",\n" +
            "    \"access_token\": \"987654321\",\n" +
            "    \"realm\": \"/employees\"\n" +
            "}";

    private static final String TOKENINFO_RESPONSE_INSUFFICIENT_SCOPES = "{\n" +
            "    \"uid\": \"testapp\",\n" +
            "    \"scope\": [\n" +
            "        \"uid\"\n" +
            "    ],\n" +
            "    \"expires_in\": 99999,\n" +
            "    \"token_type\": \"Bearer\",\n" +
            "    \"access_token\": \"987654321\",\n" +
            "    \"realm\": \"/services\"\n" +
            "}";

    private static final String EXPIRED_ACCESS_TOKEN_RESPONSE = "{\n" +
            "    \"error\": \"invalid_request\",\n" +
            "    \"error_description\": \"Access Token not valid\"\n" +
            "}";
    @Rule
    public WireMockRule wireMock = new WireMockRule(Integer.valueOf(System.getProperty("wiremock.port", "10080")));

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Before
    public void setUpMockTokenInfo() throws Exception {
        stubFor(get(urlEqualTo("/tokeninfo"))
                .withHeader(AUTHORIZATION, equalTo(USER1_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON_VALUE)
                        .withBody(USER1_TOKENINFO_RESPONSE)));

        stubFor(get(urlEqualTo("/tokeninfo"))
                .withHeader(AUTHORIZATION, equalTo(USER2_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON_VALUE)
                        .withBody(USER2_TOKENINFO_RESPONSE)));

        stubFor(get(urlEqualTo("/tokeninfo"))
                .withHeader(AUTHORIZATION, equalTo(INVALID_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(BAD_REQUEST.value())
                        .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON_VALUE)
                        .withBody(EXPIRED_ACCESS_TOKEN_RESPONSE)));


        stubFor(get(urlEqualTo("/tokeninfo"))
                .withHeader(AUTHORIZATION, equalTo(INSUFFICIENT_SCOPES_ACCESS_TOKEN))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader(ContentTypeHeader.KEY, APPLICATION_JSON_VALUE)
                        .withBody(TOKENINFO_RESPONSE_INSUFFICIENT_SCOPES)));
    }
}
