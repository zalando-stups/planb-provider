package org.zalando.planb.provider;

public class RealmAuthenticationException extends Exception {

    public RealmAuthenticationException() {
        super();
    }

    public RealmAuthenticationException(String message) {
        super(message);
    }
}
