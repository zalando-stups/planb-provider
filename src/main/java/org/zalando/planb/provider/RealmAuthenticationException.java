package org.zalando.planb.provider;

/**
 * If identity doesn't exist or secret is wrong
 */
public class RealmAuthenticationException extends RestException {

    public RealmAuthenticationException(String identity, String realm, String errorType) {
        super(401, "Identity " + identity + " in realm " + realm + " could not be authenticated.", errorType);
    }
}
