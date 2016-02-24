package org.zalando.planb.provider;

public class UserRealmAuthorizationException extends RealmAuthorizationException {

    public UserRealmAuthorizationException(String identity, String realm, Iterable<String> scopes) {
        super(identity, realm, USER_ERROR_TYPE, scopes);
    }
}
