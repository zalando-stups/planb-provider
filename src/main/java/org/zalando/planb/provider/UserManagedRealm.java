package org.zalando.planb.provider;

import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;

import java.util.Optional;

import static java.lang.String.format;

public interface UserManagedRealm extends UserRealm {

    default void update(String username, User data) throws NotFoundException {
        final User existing = get(username).orElseThrow(() -> new NotFoundException(format("Could not find user %s in realm %s", username, getName())));

        final User update = new User();
        update.setPasswordHashes(Optional.ofNullable(data.getPasswordHashes()).filter(list -> !list.isEmpty()).orElseGet(existing::getPasswordHashes));
        update.setScopes(Optional.ofNullable(data.getScopes()).orElseGet(existing::getScopes));

        createOrReplace(username, update);
    }

    void delete(String username) throws NotFoundException;

    void createOrReplace(String username, User user);

    void addPassword(String username, Password password);

    Optional<User> get(String username);

}
