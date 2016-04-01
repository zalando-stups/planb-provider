package org.zalando.planb.provider;

import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.delete;
import static org.springframework.http.RequestEntity.get;

@ActiveProfiles("it")
public class ConsentControllerIT extends AbstractOauthTest {
    private static final String SCOPE_1 = "scope1";
    private static final String SCOPE_2 = "scope2";
    private static final ImmutableSet<String> SCOPES = ImmutableSet.of(SCOPE_1, SCOPE_2);
    private static final String TEST_USERNAME = "test";
    private static final String TEST_REALM = "realm";
    private static final String TEST_CLIENT = "client";

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private Session session;

    @Autowired
    private CassandraConsentService cassandraConsentService;

    @Before
    public void storeConsent() {
        cassandraConsentService.store(TEST_USERNAME, TEST_REALM, TEST_CLIENT, SCOPES);
    }

    @After
    public void cleanupConsent() {
        cassandraConsentService.withdraw(TEST_USERNAME, TEST_REALM, TEST_CLIENT);
    }

    @Test
    public void testGetAndDeleteConsents() throws Exception {
        final URI uri = URI.create(String.format("%s/%s/%s/%s", getConsentsBaseUri(), TEST_REALM, TEST_USERNAME, TEST_CLIENT));

        assertThat(readConsent(TEST_USERNAME, TEST_REALM, TEST_CLIENT)).isNotEmpty();

        // consents were found
        ResponseEntity<String> response = getRestTemplate().exchange(get(uri).header(AUTHORIZATION, USER1_ACCESS_TOKEN)
                    .build(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().equals("{\"scopes\":[\"scope1\",\"scope2\"]}"));

        // consents were revoked
        assertThat(getRestTemplate().exchange(delete(uri).header(AUTHORIZATION, USER1_ACCESS_TOKEN).build(), Void.class)
                .getStatusCode()).isEqualTo(NO_CONTENT);

    }

    private Set<String> readConsent(final String username, final String realm, final String client) {
        return cassandraConsentService.getConsentedScopes(username, realm, client);
    }
}
