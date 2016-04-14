package org.zalando.planb.provider.realms;

import static java.lang.String.format;
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

    public static ClientRealmAuthenticationException wrongClientSecret(String clientId, String realmName) {
        return new ClientRealmAuthenticationException(
                UNAUTHORIZED.value(),
                format("Client '%s' in realm '%s' presented wrong credentials", clientId, realmName),
                "invalid_client",
                "Client authentication failed");
    }
}
