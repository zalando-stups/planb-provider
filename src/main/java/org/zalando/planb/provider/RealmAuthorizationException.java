package org.zalando.planb.provider;

/**
 * If scopes are not allowed.
 */
public class RealmAuthorizationException extends RestException {

    public RealmAuthorizationException(String message) {
        super(403, message);
    }
}
