package org.zalando.planb.provider;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class InMemoryRealm implements ManagedRealm {
    @Override
    public Map<String, Object> authenticate(final String user, final String password, final String[] scopes)
            throws RealmAuthenticationFailedException {
        if (!password.equals("test")) {
            throw new RealmAuthenticationFailedException();
        }

        return new HashMap<String, Object>() {{
            put("uid", user);
        }};
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
