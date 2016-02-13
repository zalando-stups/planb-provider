package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Client;

import java.util.Optional;

import static java.lang.String.format;

public interface ClientManagedRealm extends ClientRealm {

    default void update(String clientId, Client data) throws NotFoundException {
        final Client existing = get(clientId).orElseThrow(() -> new NotFoundException(format("Could not find client %s in realm %s", clientId, getName())));

        final Client update = new Client();
        update.setSecretHash(Optional.ofNullable(data.getSecretHash()).orElseGet(existing::getSecretHash));
        update.setScopes(Optional.ofNullable(data.getScopes()).filter(set -> !set.isEmpty()).orElseGet(existing::getScopes));
        update.setIsConfidential(Optional.ofNullable(data.getIsConfidential()).orElseGet(existing::getIsConfidential));

        createOrReplace(clientId, update);
    }

    void delete(String clientId) throws NotFoundException;

    void createOrReplace(String id, Client client);

    Optional<Client> get(String clientId);
}
