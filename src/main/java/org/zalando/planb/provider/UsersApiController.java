package org.zalando.planb.provider;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.planb.provider.api.Password;
import org.zalando.planb.provider.api.User;
import org.zalando.planb.provider.api.UsersApi;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class UsersApiController implements UsersApi {

    private final Logger log = getLogger(getClass());

    @Override
    public ResponseEntity<Void> usersRealmIdPut(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody User user) {
        log.info("Create or replace user /{}/{}: {}", realm, id, user);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> usersRealmIdDelete(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id) {
        log.info("Delete user /{}/{}", realm, id);
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
}
