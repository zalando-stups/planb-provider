package org.zalando.planb.provider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RealmConfig implements BeanFactoryAware {
    private final Map<String, ClientRealm> clientRealms = new HashMap<>();
    private final Map<String, UserRealm> userRealms = new HashMap<>();
    private BeanFactory beanFactory;

    private static final Pattern HOST_WORD_BOUNDARY = Pattern.compile("[.-]");

    public static String ensureLeadingSlash(String realmName) {
        return realmName.startsWith("/") ? realmName : "/" + realmName;
    }

    public static String stripLeadingSlash(String realm) {
        return realm.startsWith("/") ? realm.substring(1) : realm;
    }

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
        newRealm("/services", CassandraClientRealm.class, CassandraUserRealm.class);
        newRealm("/customers", CassandraClientRealm.class, CustomerUserRealm.class);
        newRealm("/employees", CassandraClientRealm.class, UpstreamUserRealm.class);
    }

    static Optional<String> findRealmNameInHost(@NotNull final Set<String> realmNames, @NotNull final String host) {
        Set<String> hostParts = ImmutableSet.copyOf(HOST_WORD_BOUNDARY.split(host));
        Optional<String> realmFromHost = realmNames.stream()
                .filter(realm -> hostParts.contains(stripLeadingSlash(realm)))
                .sorted()
                .findFirst();
        return realmFromHost;
    }

    Optional<String> findRealmNameInHost(@NotNull final String host) {
        return findRealmNameInHost(clientRealms.keySet(), host);
    }

    UserRealm getUserRealm(String name) {
        return userRealms.get(name);
    }

    ClientRealm getClientRealm(String name) {
        return clientRealms.get(name);
    }
}
