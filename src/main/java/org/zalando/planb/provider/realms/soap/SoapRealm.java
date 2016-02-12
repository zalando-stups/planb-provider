package org.zalando.planb.provider.realms.soap;

import java.util.Map;

import org.zalando.planb.provider.Realm;
import org.zalando.planb.provider.RealmAuthenticationFailedException;
import org.zalando.planb.provider.realms.Named;

import com.google.common.collect.Maps;

class SoapRealm implements Realm, Named {

    private final SoapRealmConfiguration config;

    SoapRealm(SoapRealmConfiguration config) {
        this.config = config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public Map<String, Object> authenticate(String user, String password, String[] scopes)
            throws RealmAuthenticationFailedException {
        return Maps.newHashMap();
    }

}
