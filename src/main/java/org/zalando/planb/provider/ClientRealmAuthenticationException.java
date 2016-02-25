package org.zalando.planb.provider;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

public class ClientRealmAuthenticationException extends RealmAuthenticationException {

    public ClientRealmAuthenticationException(int statusCode, String message, String errorType, String errorDescription) {
        super(statusCode, message, CLIENT_ERROR, errorType, errorDescription);
    }

    public static ClientRealmAuthenticationException clientNotFound(String clientId, String realmName) {
        return new ClientRealmAuthenticationException(
                UNAUTHORIZED.value(),
                format("Client '%s' could not be found in realm '%s'", clientId, realmName),
                "invalid_client",
                "Client authentication failed");
    }

    public static ClientRealmAuthenticationException clientIsPublic(String clientId, String realmName) {
        return new ClientRealmAuthenticationException(
                BAD_REQUEST.value(),
                format("Public client '%s' in realm '%s' is not permitted to authenticate", clientId, realmName),
                "unauthorized_client",
                "Client not authorized to use this grant type");
    }

    public static ClientRealmAuthenticationException wrongClientSecret(String clientId, String realmName) {
        return new ClientRealmAuthenticationException(
                UNAUTHORIZED.value(),
                format("Client '%s' in realm '%s' presented wrong credentials", clientId, realmName),
                "invalid_client",
                "Client authentication failed");
    }
}
