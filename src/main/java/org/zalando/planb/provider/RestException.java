package org.zalando.planb.provider;

public class RestException extends RuntimeException {
    private int statusCode;

    public RestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
