package org.zalando.planb.provider;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class RealmNotManagedException extends RestException {

    public RealmNotManagedException(String realmName) {
        super(BAD_REQUEST.value(),
                "Realm " + realmName + " is not a managed realm.",
                null,
                "realm_not_managed",
                "Realm " + realmName + " is not a managed realm.");
    }
}
