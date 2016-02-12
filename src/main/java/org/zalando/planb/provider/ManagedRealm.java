package org.zalando.planb.provider;

public interface ManagedRealm extends Realm {

    void create();
    void update();
    void delete();

}
