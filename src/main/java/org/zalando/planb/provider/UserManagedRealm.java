package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.zalando.planb.provider.Realm.checkBCryptPassword;
import static org.zalando.planb.provider.UserRealmAuthenticationException.userNotFound;
import static org.zalando.planb.provider.UserRealmAuthenticationException.wrongUserSecret;

public interface UserManagedRealm extends UserRealm {

    @Override
    default Map<String, Object> authenticate(String username, String password, Set<String> scopes, Set<String> defaultScopes)
            throws UserRealmAuthenticationException, UserRealmAuthorizationException {
        final User user = get(username).orElseThrow(() -> userNotFound(username, getName()));

        if (!user.getPasswordHashes().stream().anyMatch(passwordHash -> checkBCryptPassword(password, passwordHash))) {
            throw wrongUserSecret(username, getName());
        }

        final Set userScopes = ((Map) user.getScopes()).keySet();
        final Set<String> missingScopes = scopes.stream()
                .filter(scope -> !defaultScopes.contains(scope))
                .filter(scope -> !userScopes.contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new UserRealmAuthorizationException(username, getName(), missingScopes);
        }

        return singletonMap(UID, username);
    }

    default void update(String username, User data) throws NotFoundException {
        final User existing = get(username).orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())));

        final User update = new User();
        update.setPasswordHashes(Optional.ofNullable(data.getPasswordHashes())
                .filter(list -> !list.isEmpty()).orElseGet(existing::getPasswordHashes));

        update.setScopes(Optional.ofNullable(data.getScopes())
                .filter(scopes -> scopes instanceof Map && !((Map) scopes).isEmpty())
                .orElseGet(existing::getScopes));

        createOrReplace(username, update);
    }

    void delete(String username) throws NotFoundException;

    void createOrReplace(String username, User user);

    void addPassword(String username, Password password);

    Optional<User> get(String username);

}
