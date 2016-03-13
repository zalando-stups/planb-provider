package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.util.stream.Collectors.joining;

@ConfigurationProperties(prefix = "scope")
public class ScopeProperties {

    public static final String SPACE = " ";

    /**
     * List of default scopes per realm. Scopes are separated by a space character %20.
     *
     * CASE_INSENSITIVE TreeMap to support using an env var like "SCOPE_DEFAULTS_MYREALM=uid"
     */
    private Map<String, String> defaults = new TreeMap<>(CASE_INSENSITIVE_ORDER);

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

    public static String join(Set<String> scopes) {
        return scopes.stream().sorted().collect(joining(SPACE));
    }

    private static Set<String> splitScope(String scope) {
        return ImmutableSet.copyOf(scope.split(SPACE));
    }

}
