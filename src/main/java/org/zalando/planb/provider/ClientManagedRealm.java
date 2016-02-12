package org.zalando.planb.provider;

public interface ClientManagedRealm extends ClientRealm {

    void create();
    void update();
    void delete();

}
