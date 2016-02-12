package org.zalando.planb.provider;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class InMemoryUserRealm implements UserManagedRealm {
    @Override
    public Map<String, Object> authenticate(final String username, final String password, final String[] scopes)
            throws RealmAuthenticationException {
        if (!password.equals("test")) {
            throw new RealmAuthenticationException("user " + username + " presented wrong secret");
        }

        return new HashMap<String, Object>() {{
            put("sub", username);
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
