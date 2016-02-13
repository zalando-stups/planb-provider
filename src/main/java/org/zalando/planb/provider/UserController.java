package org.zalando.planb.provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;
import org.zalando.planb.provider.api.UsersApi;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class UserController implements UsersApi {

    private final Logger log = getLogger(getClass());

    private final RealmConfig realms;

    @Autowired
    public UserController(RealmConfig realms) {
        this.realms = realms;
    }

    @Override
    public ResponseEntity<Void> usersRealmIdPut(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody User user) {
        log.info("Create or replace user /{}/{}: {}", realm, id, user);
        getUserManagedRealm(realm).createOrReplace(id, user);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> usersRealmIdDelete(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id) {
        log.info("Delete user /{}/{}", realm, id);
        getUserManagedRealm(realm).delete(id);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> usersRealmIdPatch(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody User user) {
        log.info("Update user /{}/{}: {}", realm, id, user);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> usersRealmIdPasswordPost(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody Password password) {
        log.info("Add user password /{}/{}: {}", realm, id, password);
        return new ResponseEntity<>(CREATED);
    }

    private UserManagedRealm getUserManagedRealm(final String realm) {
        return Optional.of(realm)
                .map(RealmConfig::ensureLeadingSlash)
                .map(realms::getUserRealm)
                .filter(r -> r instanceof UserManagedRealm)
                .map(r -> ((UserManagedRealm) r))
                .orElseThrow(() -> new RealmNotManagedException(realm));
    }
}
