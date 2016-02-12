package org.zalando.planb.provider;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.planb.provider.api.Client;
import org.zalando.planb.provider.api.ClientsApi;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.OK;

@RestController
public class ClientsApiController implements ClientsApi {

    private final Logger log = getLogger(getClass());

    @Override
    public ResponseEntity<Void> clientsRealmIdPut(String realm, String id, Client client) {
        log.info("Create or replace client /{}/{}: {}", realm, id, client);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> clientsRealmIdDelete(String realm, String id) {
        log.info("Delete client /{}/{}", realm, id);
        return new ResponseEntity<>(OK);
    }

    @Override
    public ResponseEntity<Void> clientsRealmIdPatch(String realm, String id, Client client) {
        log.info("Update client /{}/{}: {}", realm, id, client);
        return new ResponseEntity<>(OK);
    }
}
