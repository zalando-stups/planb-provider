package org.zalando.planb.provider;

public class ClientRealmAuthorizationException extends RealmAuthorizationException {

    public ClientRealmAuthorizationException(String identity, String realm, String... scopes) {
        super(identity, realm, CLIENT_ERROR_TYPE, scopes);
    }
}
