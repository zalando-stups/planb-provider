package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

@ConfigurationProperties(prefix = "scope")
public class ScopeProperties {

    public static final String SPACE = " ";

    /**
     * List of default scopes per realm. Scopes are seprated by a space character %20
     */
    private Map<String, String> defaults = newHashMap();

    public Set<String> getDefaultScopes(String realm) {
        return split(Optional.ofNullable(realm)
                .map(RealmConfig::stripLeadingSlash)
                .map(defaults::get));
    }

    @SuppressWarnings("unused")
    public Map<String, String> getDefaults() {
        return defaults;
    }

    public static Set<String> split(Optional<String> scope) {
        return scope
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ScopeProperties::splitScope)
                .orElseGet(Collections::emptySet);
    }

    private static Set<String> splitScope(String scope) {
        return ImmutableSet.copyOf(scope.split(SPACE));
    }

}
