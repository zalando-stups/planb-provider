package org.zalando.planb.provider;

public class RealmNotManagedException extends RestException {

    private final String realmName;

    public RealmNotManagedException(String realmName) {
        super(400, realmName + " is not a managed realm");
        this.realmName = realmName;
    }

    public String getRealmName() {
        return realmName;
    }
}
