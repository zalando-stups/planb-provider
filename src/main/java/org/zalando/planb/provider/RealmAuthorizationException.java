package org.zalando.planb.provider;

/**
 * If scopes are not allowed.
 */
public class RealmAuthorizationException extends Exception {

    public RealmAuthorizationException(String message) {
        super(message);
    }
}
