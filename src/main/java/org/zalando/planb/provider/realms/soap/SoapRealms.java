package org.zalando.planb.provider.realms.soap;

import java.util.Map;

import com.google.common.collect.Maps;

class SoapRealms {

    private Map<String, SoapRealm> map = Maps.newHashMap();

    void add(SoapRealm realm) {
        map.put(realm.getName(), realm);
    }

    boolean contains(String realm) {
        return map.containsKey(realm);
    }

    SoapRealm get(String realm) {
        return map.get(realm);
    }

}
