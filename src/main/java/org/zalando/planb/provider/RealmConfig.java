package org.zalando.planb.provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class RealmConfig implements BeanFactoryAware {
    private final Map<String,ClientRealm> clientRealms = new HashMap<>();
    private final Map<String,UserRealm> userRealms = new HashMap<>();
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    void newRealm(String realmName, Class<? extends ClientRealm> clientRealmImpl, Class<? extends UserRealm> userRealmImpl) {
        ClientRealm clientRealm = beanFactory.getBean(clientRealmImpl);
        clientRealm.initialize(realmName);
        clientRealms.put(realmName, clientRealm);

        UserRealm userRealm = beanFactory.getBean(userRealmImpl);
        userRealm.initialize(realmName);
        userRealms.put(realmName, userRealm);
    }

    @PostConstruct
    void setup() {
        newRealm("/test", InMemoryClientRealm.class, InMemoryUserRealm.class);
        newRealm("/services", CassandraClientRealm.class, CassandraUserRealm.class);
        newRealm("/customers", CassandraClientRealm.class, CustomerLoginUserRealm.class);
    }

    UserRealm getUserRealm(String name) {
        return userRealms.get(name);
    }

    ClientRealm getClientRealm(String name) {
        return clientRealms.get(name);
    }
}
