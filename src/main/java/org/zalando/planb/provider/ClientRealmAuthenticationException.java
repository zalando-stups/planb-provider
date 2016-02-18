package org.zalando.planb.provider;

public class ClientRealmAuthenticationException extends RealmAuthenticationException {

    public ClientRealmAuthenticationException(String identity, String realm) {
        super(identity, realm, CLIENT_ERROR_TYPE);
    }
}
