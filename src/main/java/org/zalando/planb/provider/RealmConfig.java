package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class RealmConfig {
    private final Map<String,Realm> realms = new HashMap<>();

    @Autowired
    private CustomerLoginRealm customerLoginRealm;

    @Autowired
    private InMemoryRealm testRealm;

    @PostConstruct
    void setup() {
        // find a nicer way to initialize as beans and provide configuration
        realms.put("/test", testRealm);
        realms.put("/customers", customerLoginRealm);
    }

    Realm get(String name) {
        return realms.get(name);
    }
}
