package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("it")
public class TokenLifetimeIT extends AbstractOauthTest {

    @Autowired
    private RealmProperties realmProperties;

    @Test
    public void testTokenLifetimeIsOverriden() {
        ResponseEntity<OIDCCreateTokenResponse> response = createToken("/services",
                "testclient", "test", "testuser", "test", "uid ascope");

        assertThat(response.getBody().getExpiresIn()).isEqualTo(3600);
    }

    @Test
    public void testTokenLifetimeFallsbackToDefault() {
        assertThat(realmProperties.getTokenLifetime("/foo")).isEqualTo(Duration.ofHours(8));
    }
}
