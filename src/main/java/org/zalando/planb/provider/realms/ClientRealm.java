package org.zalando.planb.provider.realms;

import org.zalando.planb.provider.ClientData;

import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public interface ClientRealm extends Realm {

    default void validateScopes(String clientId, ClientData client, Set<String> requestedScopes, Set<String> defaultScopes) {
        final Set<String> missingScopes = requestedScopes.stream()
                .filter(scope -> !defaultScopes.contains(scope))
                .filter(scope -> !client.getScopes().contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new ClientRealmAuthorizationException(clientId, getName(), missingScopes);
        }
    }

    void authenticate(String clientId, String clientSecret, Set<String> scopes, Set<String> defaultScopes)
            throws ClientRealmAuthenticationException, ClientRealmAuthorizationException;

    Optional<ClientData> get(String clientId);
}
