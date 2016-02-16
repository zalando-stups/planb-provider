package org.zalando.planb.provider;

/**
 * If identity doesn't exist or secret is wrong
 */
public class RealmAuthenticationException extends RestException {

    public RealmAuthenticationException(String message) {
        super(401, message);
    }
}
