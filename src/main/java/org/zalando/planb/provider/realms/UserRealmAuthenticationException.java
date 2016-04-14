package org.zalando.planb.provider.realms;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class UserRealmAuthenticationException extends RealmAuthenticationException {

    public UserRealmAuthenticationException(String message) {
        super(BAD_REQUEST.value(), message, USER_ERROR, "invalid_grant", "The provided access grant is invalid, expired, or revoked.");
    }

    public static UserRealmAuthenticationException userNotFound(String username, String realmName) {
        return new UserRealmAuthenticationException(format("User '%s' could not be found in realm '%s'", username, realmName));
    }

    public static UserRealmAuthenticationException wrongUserSecret(String username, String realmName) {
        return new UserRealmAuthenticationException(
                format("User '%s' in realm '%s' presented wrong credentials", username, realmName));
    }
}
