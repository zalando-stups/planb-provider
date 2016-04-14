package org.zalando.planb.provider;

import java.util.Set;

public interface ConsentService {

    void store(final String username, final String realm, final String clientId, final Set<String> scopes);

    Set<String> getConsentedScopes(final String username, final String realm, final String clientId);

    void withdraw(final String username, final String realm, final String clientId);
}
