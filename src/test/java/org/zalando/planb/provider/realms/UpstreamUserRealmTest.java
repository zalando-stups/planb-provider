package org.zalando.planb.provider.realms;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zalando.planb.provider.RestException;
import org.zalando.planb.provider.realms.UpstreamRealmProperties;
import org.zalando.planb.provider.realms.UpstreamTokenResponse;
import org.zalando.planb.provider.realms.UpstreamUserRealm;
import org.zalando.planb.provider.realms.UserRealmAuthenticationException;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.fail;

/**
 * Created by hjacobs on 07.03.16.
 */
public class UpstreamUserRealmTest {

    private int wiremockPort = Integer.valueOf(System.getProperty("wiremock.port", "10080"));

    @Rule
    public WireMockRule wireMock = new WireMockRule(wiremockPort);

    @Before
    public void setUpMocks() throws Exception {
        stubFor(get(urlPathEqualTo("/token")).willReturn(aResponse()
                .withStatus(200)
                .withBody("mytok\n")));
        stubFor(get(urlPathEqualTo("/token-invalid")).willReturn(aResponse()
                .withStatus(401)
                .withBody("invalid\n")));
        stubFor(get(urlPathEqualTo("/tokeninfo")).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"uid\":\"myuid\"}")));
        stubFor(get(urlPathEqualTo("/tokeninfo-invalid")).willReturn(aResponse()
                .withStatus(401)));
    }

    @Test
    public void testInitialize() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        UpstreamUserRealm realm = new UpstreamUserRealm(props);
        realm.initialize("myrealm");
        assertThat(realm.getName()).isEqualTo("myrealm");
        assertThat(realm.maskSubject("mysub")).isEqualTo("mysub");
    }

    @Test
    public void testGetAccessToken() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        props.setTokenServiceUrl("http://localhost:" + wiremockPort + "/token");
        UpstreamUserRealm realm = new UpstreamUserRealm(props);

        String token = realm.getAccessToken("myuser", "mypass", ImmutableSet.of("myscope"));
        assertThat(token).isEqualTo("mytok");
    }

    @Test
    public void testGetTokenInfo() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        props.setTokenInfoUrl("http://localhost:" + wiremockPort + "/tokeninfo");
        UpstreamUserRealm realm = new UpstreamUserRealm(props);

        UpstreamTokenResponse response = realm.getTokenInfo("myuser", "1234");
        assertThat(response.getUid()).isEqualTo("myuid");
    }

    @Test
    public void testAuthenticate() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        props.setTokenServiceUrl("http://localhost:" + wiremockPort + "/token");
        props.setTokenInfoUrl("http://localhost:" + wiremockPort + "/tokeninfo");
        UpstreamUserRealm realm = new UpstreamUserRealm(props);

        Map<String, String> response = realm.authenticate("myuser", "mypass", ImmutableSet.of("myscope"), ImmutableSet.of("myscope"));
        assertThat(response).containsEntry("sub", "myuid");
    }

    @Test
    public void testAuthenticateWrongCredentials() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        props.setTokenServiceUrl("http://localhost:" + wiremockPort + "/token-invalid");
        props.setTokenInfoUrl("http://localhost:" + wiremockPort + "/tokeninfo");
        UpstreamUserRealm realm = new UpstreamUserRealm(props);

        try {
            realm.authenticate("myuser", "mypass", ImmutableSet.of("myscope"), ImmutableSet.of("myscope"));
            fail("Authenticate should have thrown exception");
        } catch (UserRealmAuthenticationException ex) {
            assertThat(ex.getErrorType()).isEqualTo("invalid_grant");
            assertThat(ex.getErrorLocation()).contains(RestException.USER_ERROR);
            assertThat(ex.getMessage()).isEqualTo("User myuser login failed");
        }
    }

    @Test
    public void testAuthenticateTokenInfoFails() {
        UpstreamRealmProperties props = new UpstreamRealmProperties();
        props.setTokenServiceUrl("http://localhost:" + wiremockPort + "/token");
        props.setTokenInfoUrl("http://localhost:" + wiremockPort + "/tokeninfo-invalid");
        UpstreamUserRealm realm = new UpstreamUserRealm(props);

        try {
            realm.authenticate("myuser", "mypass", ImmutableSet.of("myscope"), ImmutableSet.of("myscope"));
            fail("Authenticate should have thrown exception");
        } catch (UserRealmAuthenticationException ex) {
            assertThat(ex.getErrorType()).isEqualTo("invalid_grant");
            assertThat(ex.getErrorLocation()).contains(RestException.USER_ERROR);
            assertThat(ex.getMessage()).isEqualTo("User myuser login failed");
        }
    }
}
