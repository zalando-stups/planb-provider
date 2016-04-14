package org.zalando.planb.provider.realms;

import org.zalando.planb.provider.RestException;

import static java.lang.String.join;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * If scopes are not allowed.
 */
public class RealmAuthorizationException extends RestException {

    public RealmAuthorizationException(String identity, String realm, String errorLocation, Iterable<String> invalidScopes) {
        super(BAD_REQUEST.value(), buildMessage(identity, realm, invalidScopes), errorLocation, "invalid_scope", buildDescription(invalidScopes));
    }

    private static String buildMessage(String identity, String realm, Iterable<String> scopes) {
        return "Identity " + identity + " in realm " + realm + " is not authorized to request scope: " + join(" ", scopes);
    }

    private static String buildDescription(Iterable<String> scopes) {
        return "Unknown/invalid scope(s): [" + join(", ", scopes) + "]";
    }
}
