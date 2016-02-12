package org.zalando.planb.provider.realms;

import org.springframework.plugin.core.Plugin;
import org.zalando.planb.provider.Realm;

public interface RealmPlugin extends Plugin<String> {

    Realm get(String name);

}
