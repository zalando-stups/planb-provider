package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Client;

public interface ClientManagedRealm extends ClientRealm {

    void update();

    void delete(String clientId) throws NotFoundException;

    void createOrReplace(String id, Client client);
}
