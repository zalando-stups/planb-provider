package org.zalando.planb.provider;

import static org.slf4j.LoggerFactory.getLogger;

import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.zalando.planb.provider.OIDCController.getRealmName;

import java.util.ArrayList;
import java.util.Set;

import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import org.zalando.planb.provider.api.Consent;
import org.zalando.planb.provider.api.ConsentsApi;

@RestController
public class ConsentController implements ConsentsApi {

    private final Logger log = getLogger(getClass());

    @Autowired
    private CassandraConsentService cassandraConsentService;

    @Autowired
    private RealmConfig realms;

    @Override
    public ResponseEntity<Consent> consentsRealmUsernameClientIdGet(@PathVariable("realm") final String realm,
            @PathVariable("username") final String username,
            @PathVariable("client_id") final String clientId) {
        log.info("Get stored consents for user {} on realm {}, application id {}", username, realm, clientId);
        final String realmName = getRealmName(realms, realm);
        final Consent consentedScopes = new Consent();

        final Set<String> scopes = cassandraConsentService.getConsentedScopes(username, realmName, clientId);
        consentedScopes.setScopes(new ArrayList<>(scopes));

        return ResponseEntity.ok(consentedScopes);
    }

    @Override
    public ResponseEntity<Void> consentsRealmUsernameClientIdDelete(@PathVariable("username") final String username,
            @PathVariable("realm") final String realm,
            @PathVariable("client_id") final String clientId) {
        log.info("Withdrawing stored consents for user {} on realm {}, application id {}", username, realm, clientId);
        final String realmName = getRealmName(realms, realm);

        cassandraConsentService.withdraw(username, realmName, clientId);

        return new ResponseEntity<>(NO_CONTENT);
    }
}
