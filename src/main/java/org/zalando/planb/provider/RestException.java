package org.zalando.planb.provider;

import java.util.Optional;

public class RestException extends RuntimeException {

    public static final String CLIENT_ERROR_TYPE = "client";
    public static final String USER_ERROR_TYPE = "user";

    private final int statusCode;

    private final Optional<String> realmType;

    public RestException(int statusCode, String message) {
        this(statusCode, message, null);
    }

    public RestException(int statusCode, String message, String errorType) {
        super(message);
        this.statusCode = statusCode;
        this.realmType = Optional.ofNullable(errorType);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<String> getErrorType() {
        return realmType;
    }
}
