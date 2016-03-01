package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.zalando.planb.provider.api.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

public class UserData {

    private final Set<UserPasswordHash> passwordHashes;
    private final Map<String, String> scopes;
    private final String createdBy;
    private final String lastModifiedBy;

    public UserData(Set<UserPasswordHash> passwordHashes, Map<String, String> scopes, String createdBy, String lastModifiedBy) {
        this.passwordHashes = passwordHashes;
        this.scopes = scopes;
        this.createdBy = createdBy;
        this.lastModifiedBy = lastModifiedBy;
    }

    public Set<UserPasswordHash> getPasswordHashes() {
        return passwordHashes;
    }

    public Map<String, String> getScopes() {
        return scopes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public static Builder copyOf(User user) {
        return new Builder()
                .withPasswordHashes(user.getPasswordHashes())
                .withScopes(scopesMap(user));
    }

    private static Map<String, String> scopesMap(User user) {
        final Map<?, ?> scopes = (Map<?, ?>) user.getScopes();
        if (scopes == null) {
            return emptyMap();
        } else {
            final Map<String, String> result = newHashMapWithExpectedSize(scopes.size());
            scopes.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
            return result;
        }
    }

    public static class Builder {

        private Set<UserPasswordHash> passwordHashes;
        private Map<String, String> scopes;
        private String createdBy;
        private String lastModifiedBy;

        public UserData build() {
            return new UserData(passwordHashes, scopes, createdBy, lastModifiedBy);
        }

        public Builder withPasswordHashes(Collection<String> passwordHashes) {
            this.passwordHashes = passwordHashes.stream().map(UserPasswordHash::new).collect(toSet());
            return this;
        }

        public Builder withPasswordHashes(Iterable<UserPasswordHash> passwordHashes) {
            this.passwordHashes = ImmutableSet.copyOf(passwordHashes);
            return this;
        }

        public Builder withScopes(Map<String, String> scopes) {
            this.scopes = scopes;
            return this;
        }

        public Builder withCreatedBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder withLastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }
    }
}
