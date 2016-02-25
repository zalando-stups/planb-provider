package org.zalando.planb.provider;

public class ClientRealmAuthorizationException extends RealmAuthorizationException {

    public ClientRealmAuthorizationException(String identity, String realm, Iterable<String> invalidScopes) {
        super(identity, realm, CLIENT_ERROR, invalidScopes);
    }
}
