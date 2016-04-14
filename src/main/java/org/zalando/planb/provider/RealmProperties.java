package org.zalando.planb.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.zalando.planb.provider.realms.ClientRealm;
import org.zalando.planb.provider.realms.UserRealm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

@ConfigurationProperties(prefix = "realm")
public class RealmProperties {

    private static final Duration DEFAULT_TOKEN_LIFETIME = Duration.ofHours(8);

    private List<String> names = new ArrayList<>();

    private Map<String, Class<? extends ClientRealm>> clientImpl = new TreeMap<>(CASE_INSENSITIVE_ORDER);
    private Map<String, Class<? extends UserRealm>> userImpl = new TreeMap<>(CASE_INSENSITIVE_ORDER);
    private Map<String, Duration> tokenLifetime = new TreeMap<>(CASE_INSENSITIVE_ORDER);

    public List<String> getNames() {
        return names;
    }

    public Map<String, Class<? extends ClientRealm>> getClientImpl() {
        return clientImpl;
    }

    public Class<? extends ClientRealm> getClientImpl(String realmName, Class<? extends ClientRealm> defaultImpl) {
        // try realm name also without slash (necessary to support config via simple environment variables!)
        return clientImpl.getOrDefault(realmName, clientImpl.getOrDefault(RealmConfig.stripLeadingSlash(realmName), defaultImpl));
    }

    public Map<String, Class<? extends UserRealm>> getUserImpl() {
        return userImpl;
    }

    public Class<? extends UserRealm> getUserImpl(String realmName, Class<? extends UserRealm> defaultImpl) {
        // try realm name also without slash (necessary to support config via simple environment variables!)
        return userImpl.getOrDefault(realmName, userImpl.getOrDefault(RealmConfig.stripLeadingSlash(realmName), defaultImpl));
    }

    public Map<String, Duration> getTokenLifetime() {
        return tokenLifetime;
    }

    public Duration getTokenLifetime(String realmName, Duration defaultTokenLifetime) {
        // try realm name also without slash (necessary to support config via simple environment variables!)
        return tokenLifetime.getOrDefault(realmName, tokenLifetime.getOrDefault(RealmConfig.stripLeadingSlash(realmName), defaultTokenLifetime));
    }

    public Duration getTokenLifetime(String realmName) {
        return getTokenLifetime(realmName, DEFAULT_TOKEN_LIFETIME);
    }
}
