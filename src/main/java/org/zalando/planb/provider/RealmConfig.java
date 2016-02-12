package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.realms.RealmPlugin;

@Component
public class RealmConfig {

    private final PluginRegistry<RealmPlugin, String> realmPluginRegistry;

    @Autowired
    public RealmConfig(PluginRegistry<RealmPlugin, String> realmPluginRegistry) {
        this.realmPluginRegistry = realmPluginRegistry;
    }

    Realm get(String name) {
        RealmPlugin plugin = realmPluginRegistry.getPluginFor(name);
        return plugin.get(name);
    }
}
