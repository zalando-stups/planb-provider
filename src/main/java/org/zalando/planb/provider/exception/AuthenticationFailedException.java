package org.zalando.planb.provider.exception;

public class AuthenticationFailedException extends Exception {

    public AuthenticationFailedException() {
        super();
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
