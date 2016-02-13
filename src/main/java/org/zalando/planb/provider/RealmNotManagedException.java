package org.zalando.planb.provider;

public class RealmNotManagedException extends RuntimeException {

    private final String realmName;

    public RealmNotManagedException(String realmName) {
        super(realmName + " is not a managed realm");
        this.realmName = realmName;
    }

    public String getRealmName() {
        return realmName;
    }
}
