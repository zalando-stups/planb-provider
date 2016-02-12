package org.zalando.planb.provider;

public interface ClientRealm {

    void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException;

}
