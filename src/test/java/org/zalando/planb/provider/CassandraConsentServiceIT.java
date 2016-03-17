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

    private static final String SCOPE_1 = "scope1";
    private static final String SCOPE_2 = "scope2";
    private static final ImmutableSet<String> SCOPES = ImmutableSet.of(SCOPE_1, SCOPE_2);
    private static final String TEST_USERNAME = "test";
    private static final String TEST_REALM = "realm";
    private static final String TEST_CLIENT = "client";

    @Autowired
    CassandraConsentService cassandraConsentService;

    @Test
    public void storeAndReadConsent() {
        cassandraConsentService.store(TEST_USERNAME, TEST_REALM, TEST_CLIENT, SCOPES);

        Set<String> scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, TEST_CLIENT);

        assertThat(scopes).containsExactly(SCOPE_1, SCOPE_2);
    }

    @Test
    public void readEmptyConsent() {
        Set<String> scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, "nonexistingclient");
        assertThat(scopes).isEmpty();
    }

    @Test
    public void storeAndWithdrawAllConsents() {
        cassandraConsentService.store(TEST_USERNAME, TEST_REALM, TEST_CLIENT, SCOPES);

        Set<String> scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
        assertThat(scopes).containsExactly(SCOPE_1, SCOPE_2);

        cassandraConsentService.withdraw(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
        scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
        assertThat(scopes).isEmpty();
    }

    @Test
    public void storeAndWithdrawlSingleConsent() {
        cassandraConsentService.store(TEST_USERNAME, TEST_REALM, TEST_CLIENT, SCOPES);

        Set<String> scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
        assertThat(scopes).containsExactly(SCOPE_1, SCOPE_2);

        cassandraConsentService.store(TEST_USERNAME, TEST_REALM, TEST_CLIENT, ImmutableSet.of(SCOPE_1));
        scopes = cassandraConsentService.getConsentedScopes(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
        assertThat(scopes).containsExactly(SCOPE_1);
    }

}
