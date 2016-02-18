package org.zalando.planb.provider;

public class UserRealmAuthenticationException extends RealmAuthenticationException {

    public UserRealmAuthenticationException(String identity, String realm) {
        super(identity, realm, USER_ERROR_TYPE);
    }
}
