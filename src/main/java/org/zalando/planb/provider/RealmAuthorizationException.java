package org.zalando.planb.provider;

public class RealmAuthorizationException extends Exception {

    public RealmAuthorizationException() {
        super();
    }

    public RealmAuthorizationException(String message) {
        super(message);
    }
}
