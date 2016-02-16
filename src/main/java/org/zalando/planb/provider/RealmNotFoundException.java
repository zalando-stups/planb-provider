package org.zalando.planb.provider;

public class RealmNotFoundException extends RestException {
    public RealmNotFoundException(String realmName) {
        super(400, "Realm " + realmName + " is not available.");
    }
}
