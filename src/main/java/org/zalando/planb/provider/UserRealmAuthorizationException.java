package org.zalando.planb.provider;

public class UserRealmAuthorizationException extends RealmAuthorizationException {

    public UserRealmAuthorizationException(String identity, String realm, String... scopes) {
        super(identity, realm, USER_ERROR_TYPE, scopes);
    }
}
