package org.zalando.planb.provider;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Maps.newConcurrentMap;
import static java.lang.String.format;

@Component
@Scope("prototype")
public class InMemoryUserRealm implements UserManagedRealm {

    private final ConcurrentMap<String, User> users = newConcurrentMap();

    private String realmName;

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
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public void delete(String username) throws NotFoundException {
        if (users.remove(username) == null) {
            throw new NotFoundException(format("Could not find user %s in realm %s", username, getName()));
        }
    }

    @Override
    public void createOrReplace(String username, User user) {
        users.put(username, user);
    }

    @Override
    public void addPassword(String username, Password password) {
        get(username)
                .orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())))
                .getPasswordHashes().add(password.getPasswordHash());
    }

    @Override
    public Optional<User> get(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
