package org.zalando.planb.provider;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class InMemoryClientRealm implements ClientManagedRealm {
    @Override
    public void authenticate(final String clientId, final String clientSecret, final String[] scopes)
            throws RealmAuthenticationException {
        if (!clientSecret.equals("test")) {
            throw new RealmAuthenticationException("client " + clientId + " presented wrong secret");
        }
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }
}
