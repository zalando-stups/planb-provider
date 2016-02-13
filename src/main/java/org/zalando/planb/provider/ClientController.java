package org.zalando.planb.provider;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.planb.provider.api.Client;
import org.zalando.planb.provider.api.ClientsApi;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class ClientController implements ClientsApi {

    private final Logger log = getLogger(getClass());

    private final RealmConfig realms;

    @Autowired
    public ClientController(RealmConfig realms) {
        this.realms = realms;
    }

    @Override
    public ResponseEntity<Void> clientsRealmIdPut(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody Client client) {
        log.info("Create or replace client /{}/{}: {}", realm, id, client);
        getClientManagedRealm(realm).createOrReplace(id, client);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> clientsRealmIdDelete(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id) throws RealmNotManagedException {
        log.info("Delete client /{}/{}", realm, id);
        getClientManagedRealm(realm).delete(id);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> clientsRealmIdPatch(
            @PathVariable("realm") String realm,
            @PathVariable("id") String id,
            @RequestBody Client client) {
        log.info("Update client /{}/{}: {}", realm, id, client);
        return new ResponseEntity<>(OK);
    }

    private ClientManagedRealm getClientManagedRealm(final String realm) {
        return Optional.of(realm)
                .map(ClientController::ensureLeadingSlash)
                .map(realms::getClientRealm)
                .filter(r -> r instanceof ClientManagedRealm)
                .map(r -> ((ClientManagedRealm) r))
                .orElseThrow(() -> new RealmNotManagedException(realm));
    }

    private static String ensureLeadingSlash(String realmName) {
        return realmName.startsWith("/") ? realmName : "/" + realmName;
    }
}
