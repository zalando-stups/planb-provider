package org.zalando.planb.provider;

public class RealmNotManagedException extends RestException {
    public RealmNotManagedException(String realmName) {
        super(400, "Realm " + realmName + " is not a managed realm.");
    }
}
