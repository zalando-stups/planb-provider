package org.zalando.planb.provider;

public interface ClientRealm {

    default void initialize(String realmName) {
        // noop
    }

    void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException;

}
