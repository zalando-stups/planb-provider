package org.zalando.planb.provider.realms;

import org.zalando.planb.provider.*;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.zalando.planb.provider.realms.ClientRealmAuthenticationException.*;

public interface ClientManagedRealm extends ClientRealm {

    @Override
    default void authenticate(String clientId, String clientSecret, Set<String> scopes, Set<String> defaultScopes)
            throws ClientRealmAuthenticationException, ClientRealmAuthorizationException {
        final ClientData client = get(clientId).orElseThrow(() -> clientNotFound(clientId, getName()));

        if (!client.isConfidential()) {
            // TODO: non-confidential clients have no client secret,
            // i.e. we do not really authenticate anything here
            // Consider linking clients to users for the Resource Owner Password Credentials flow?

        } else if (!Realm.checkBCryptPassword(clientSecret, client.getClientSecretHash())) {
            throw wrongClientSecret(clientId, getName());
        }

        validateScopes(clientId, client, scopes, defaultScopes);
    }

    void update(String clientId, ClientData data) throws NotFoundException;

    void delete(String clientId) throws NotFoundException;

    void createOrReplace(String id, ClientData client);


}
