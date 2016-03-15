package org.zalando.planb.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;

import org.springframework.test.context.ActiveProfiles;

import com.google.common.collect.ImmutableSet;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class CassandraConsentServiceIT extends AbstractSpringTest {

    @Autowired
    CassandraConsentService cassandraConsentService;

    @Test
    public void storeAndReadConsent() {
        cassandraConsentService.store("test", "realmtest", "client", ImmutableSet.of("scope1", "scope2"));

        Set<String> scopes = cassandraConsentService.getConsentedScopes("test", "realmtest", "client");

        assertThat(scopes).containsExactly("scope1", "scope2");
    }

    @Test
    public void readEmptyConsent() {
        Set<String> scopes = cassandraConsentService.getConsentedScopes("test", "realmtest", "nonexistingclient");

        assertThat(scopes).isEmpty();
    }

}
