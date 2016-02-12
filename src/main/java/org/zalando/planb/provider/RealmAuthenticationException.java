package org.zalando.planb.provider;

/**
 * If identity doesn't exist or secret is wrong
 */
public class RealmAuthenticationException extends Exception {

    public RealmAuthenticationException() {
        super();
    }

    public RealmAuthenticationException(String message) {
        super(message);
    }
}
