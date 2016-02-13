package org.zalando.planb.provider;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.api.Client;

import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.lang.String.format;

@Component
@Scope("prototype")
public class InMemoryClientRealm implements ClientManagedRealm {

    private final ConcurrentMap<String, Client> clients = newConcurrentMap();

    private String realmName;

    @Override
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public void authenticate(final String clientId, final String clientSecret, final String[] scopes)
            throws RealmAuthenticationException {
        if (!clientSecret.equals("test")) {
            throw new RealmAuthenticationException("client " + clientId + " presented wrong secret");
        }
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String clientId) {
        if (clients.remove(clientId) == null) {
            throw new NotFoundException(format("Could not find client %s in realm %s", clientId, realmName));
        }
    }

    @Override
    public void createOrReplace(String id, Client client) {
        clients.put(id, client);
    }


}
