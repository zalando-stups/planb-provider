package org.zalando.planb.provider;

public interface ClientRealm extends Realm {

    void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException;
}
