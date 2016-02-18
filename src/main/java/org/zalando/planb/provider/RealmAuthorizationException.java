package org.zalando.planb.provider;

/**
 * If scopes are not allowed.
 */
public class RealmAuthorizationException extends RestException {

    public RealmAuthorizationException(String identity, String realm, String errorType, String... scopes) {
        super(403, "Identity " + identity + " in realm " + realm + " is not authorized to request scope: " + String.join(" ", scopes), errorType);
    }
}
