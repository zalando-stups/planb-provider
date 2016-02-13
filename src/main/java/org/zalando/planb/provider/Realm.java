package org.zalando.planb.provider;

public interface Realm {

    default void initialize(String realmName) {
        // noop
    }

    String getName();
}
