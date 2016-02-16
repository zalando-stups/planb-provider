package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;

public interface UserManagedRealm extends UserRealm {

    default Map<String, Object> authenticate(String username, String password, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException {
        final User user = get(username).orElseThrow(() -> new RealmAuthenticationException(
                format("User %s does not exist in realm %s", username, getName())));

        final Base64.Decoder base64Decoder = Base64.getDecoder();
        if (!user.getPasswordHashes().stream()
                .map(base64Decoder::decode)
                .map(bytes -> new String(bytes, UTF_8))
                .anyMatch(passwordHash -> Realm.checkBCryptPassword(password, passwordHash))) {
            throw new RealmAuthenticationException(format("Invalid password for user %s in realm %s", username, getName()));
        }

        final Set userScopes = ((Map) user.getScopes()).keySet();
        final Set<String> missingScopes = Stream.of(scopes)
                .filter(scope -> !userScopes.contains(scope))
                .collect(toSet());

        if (!missingScopes.isEmpty()) {
            throw new RealmAuthorizationException(
                    format("User %s in realm %s is not configured for scopes %s", username, getName(), missingScopes));
        }

        return singletonMap("sub", username);
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
