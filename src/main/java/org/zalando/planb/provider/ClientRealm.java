package org.zalando.planb.provider;

import java.util.Set;

public interface ClientRealm extends Realm {

    void authenticate(String clientId, String clientSecret, Set<String> scopes, Set<String> defaultScopes)
            throws ClientRealmAuthenticationException, ClientRealmAuthorizationException;
}
