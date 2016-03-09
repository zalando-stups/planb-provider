package org.zalando.planb.provider.realms;

public class UserRealmAuthorizationException extends RealmAuthorizationException {

    public UserRealmAuthorizationException(String identity, String realm, Iterable<String> invalidScopes) {
        super(identity, realm, USER_ERROR, invalidScopes);
    }
}
