package org.zalando.planb.provider;

/**
 * If scopes are not allowed.
 */
public class RealmAuthorizationException extends RestException {
    public RealmAuthorizationException(String identity, String realm, String[] scopes) {
        this(identity, realm, String.join(" ", scopes));
    }

    public RealmAuthorizationException(String identity, String realm, String scope) {
        super(403, "Identity " + identity + " in realm " + realm + " is not authorized to request scope: " + scope);
    }
}
