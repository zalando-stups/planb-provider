package org.zalando.planb.provider;

public class RealmNotManagedException extends RuntimeException {

    public RealmNotManagedException(String realm) {
        super(realm + " is not managed");
    }
}
