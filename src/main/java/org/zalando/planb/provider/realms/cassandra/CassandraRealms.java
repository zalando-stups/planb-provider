package org.zalando.planb.provider.realms.cassandra;

import java.util.Map;

import com.google.common.collect.Maps;

class CassandraRealms {

    private Map<String, CassandraRealm> map = Maps.newHashMap();

    public void add(CassandraRealm realm) {
        map.put(realm.getName(), realm);
    }

    public boolean contains(String realm) {
        return map.containsKey(realm);
    }

    public CassandraRealm get(String realm) {
        return map.get(realm);
    }

}
