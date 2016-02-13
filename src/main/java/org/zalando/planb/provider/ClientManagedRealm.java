package org.zalando.planb.provider;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.zalando.planb.provider.api.Client;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

public interface ClientManagedRealm extends ClientRealm {

    @Override
    default void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException {
        final Client client = get(clientId)
                .filter(Client::getIsConfidential) // TODO hardcoded assumption, that ony Resource Owner Password Credentials is supported
                .filter(c -> BCrypt.checkpw(clientSecret, c.getSecretHash()))
                .orElseThrow(RealmAuthenticationException::new);

        final Set<String> missingScopes = Stream.of(scopes)
                .filter(scope -> !client.getScopes().contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new RealmAuthorizationException(
                    format("Client %s in realm %s is not configured for scopes %s", clientId, getName(), missingScopes));
        }
    }

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
