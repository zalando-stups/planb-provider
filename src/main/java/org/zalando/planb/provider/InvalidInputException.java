package org.zalando.planb.provider;

public class InvalidInputException extends RestException {

    public InvalidInputException(String message) {
        super(400, message);
    }
}
