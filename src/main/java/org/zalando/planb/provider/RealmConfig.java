package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class RealmConfig {
    private final Map<String,UserRealm> userRealms = new HashMap<>();
    private final Map<String,ClientRealm> clientRealms = new HashMap<>();

    @Autowired
    private CustomerLoginUserRealm customerLoginRealm;

    @Autowired
    private UserManagedRealm testRealm;

    @PostConstruct
    void setup() {
        // find a nicer way to initialize as beans and provide configuration
        userRealms.put("/test", testRealm);
        userRealms.put("/customers", customerLoginRealm);
    }

    UserRealm getUserRealm(String name) {
        return userRealms.get(name);
    }
}
