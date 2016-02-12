package org.zalando.planb.provider;

import java.util.Map;

public interface ManagedRealm extends Realm {

    void create();
    void update();
    void delete();

}
