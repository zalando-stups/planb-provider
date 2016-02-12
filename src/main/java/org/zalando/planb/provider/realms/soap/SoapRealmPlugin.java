package org.zalando.planb.provider.realms.soap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.Realm;
import org.zalando.planb.provider.realms.RealmPlugin;

@Component
class SoapRealmPlugin implements RealmPlugin {

    private final SoapRealms realms;

    @Autowired
    SoapRealmPlugin(SoapRealms realms) {
        this.realms = realms;
    }

    @Override
    public boolean supports(String delimiter) {
        return realms.contains(delimiter);
    }

    @Override
    public Realm get(String name) {
        return realms.get(name);
    }

}
