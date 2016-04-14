package org.zalando.planb.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

@ConfigurationProperties(prefix = "scope")
public class ScopeProperties {

    /**
     * List of default scopes per realm. Scopes are separated by a space character %20.
     *
     * CASE_INSENSITIVE TreeMap to support using an env var like "SCOPE_DEFAULTS_MYREALM=uid"
     */
    private Map<String, String> defaults = new TreeMap<>(CASE_INSENSITIVE_ORDER);

    public Map<String, String> getDefaults() {
        return defaults;
    }

}
