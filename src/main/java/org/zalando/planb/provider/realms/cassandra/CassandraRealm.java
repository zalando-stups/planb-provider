package org.zalando.planb.provider.realms.cassandra;

import java.util.Map;

import org.zalando.planb.provider.Realm;
import org.zalando.planb.provider.RealmAuthenticationFailedException;
import org.zalando.planb.provider.realms.Named;

import com.google.common.collect.Maps;

class CassandraRealm implements Realm, Named {

    private final CassandraRealmConfiguration config;

    CassandraRealm(CassandraRealmConfiguration config) {
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
