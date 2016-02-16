package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Client;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

public interface ClientManagedRealm extends ClientRealm {

    @Override
    default void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException {
        final Client client = get(clientId)
                .orElseThrow(() -> new RealmAuthenticationException(clientId, getName()));

        // TODO hardcoded assumption, that only Resource Owner Password Credentials flow is supported
        if (!client.getIsConfidential()) {
            throw new RealmAuthenticationException(clientId, getName());
        }

        final String decodedSecretHash = new String(Base64.getDecoder().decode(client.getSecretHash()), UTF_8);
        if (!Realm.checkBCryptPassword(clientSecret, decodedSecretHash)) {
            throw new RealmAuthenticationException(clientId, getName());
        }

        final Set<String> missingScopes = Stream.of(scopes)
                .filter(scope -> !client.getScopes().contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new RealmAuthorizationException(clientId, getName(), scopes);
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
