package org.zalando.planb.provider;

public class RealmAuthenticationFailedException extends Exception {

    public RealmAuthenticationFailedException() {
        super();
    }

    public RealmAuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RealmAuthenticationFailedException(String message) {
        super(message);
    }
}
