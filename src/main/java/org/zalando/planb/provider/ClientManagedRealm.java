package org.zalando.planb.provider;

import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.zalando.planb.provider.ClientRealmAuthenticationException.*;

public interface ClientManagedRealm extends ClientRealm {

    @Override
    default void authenticate(String clientId, String clientSecret, Set<String> scopes, Set<String> defaultScopes)
            throws ClientRealmAuthenticationException, ClientRealmAuthorizationException {
        final ClientData client = get(clientId).orElseThrow(() -> clientNotFound(clientId, getName()));

        // TODO hardcoded assumption, that only Resource Owner Password Credentials flow is supported
        if (!client.isConfidential()) {
            throw clientIsPublic(clientId, getName());
        }

        if (!Realm.checkBCryptPassword(clientSecret, client.getClientSecretHash())) {
            throw wrongClientSecret(clientId, getName());
        }

        final Set<String> missingScopes = scopes.stream()
                .filter(scope -> !defaultScopes.contains(scope))
                .filter(scope -> !client.getScopes().contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new ClientRealmAuthorizationException(clientId, getName(), missingScopes);
        }
    }

    void update(String clientId, ClientData data) throws NotFoundException;

    void delete(String clientId) throws NotFoundException;

    void createOrReplace(String id, ClientData client);

    Optional<ClientData> get(String clientId);
}
