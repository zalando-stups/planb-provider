package org.zalando.planb.provider;

public interface UserManagedRealm extends UserRealm {

    void create();
    void update();
    void delete();

}
