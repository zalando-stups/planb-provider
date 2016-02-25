package org.zalando.planb.provider;

import java.util.Optional;

public class RestException extends RuntimeException {

    public static final String CLIENT_ERROR = "client";
    public static final String USER_ERROR = "user";

    private final int statusCode;

    private final Optional<String> errorLocation;

    private final String errorType;

    private final String errorDescription;

    /**
     * @param statusCode       an HTTP error status code (>= 400)
     * @param message          a detailed message that will be logged, but not be included in the API response
     * @param errorLocation    "client", "user", or null. Internal stuff, just for more detailed metrics
     * @param errorType        used to populate the "error" field in the response. Should be compliant to <a href="https://tools.ietf.org/html/rfc6749#section-5.2">the RFC</a>
     * @param errorDescription used to populate the "error_description" field in the response. Optional
     */
    public RestException(int statusCode, String message, String errorLocation, String errorType, String errorDescription) {
        super(message);
        this.statusCode = statusCode;
        this.errorLocation = Optional.ofNullable(errorLocation);
        this.errorType = errorType;
        this.errorDescription = errorDescription;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Optional<String> getErrorLocation() {
        return errorLocation;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
