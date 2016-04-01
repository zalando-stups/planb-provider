package org.zalando.planb.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.zalando.planb.provider.api.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;

@Getter
@AllArgsConstructor
@Builder
public class UserData {

    private Set<UserPasswordHash> passwordHashes;
    private Map<String, String> scopes;
    private String createdBy;
    private String lastModifiedBy;

    public static UserData copyOf(final User user) {
        return builderOf(user).build();
    }

    public static UserData.UserDataBuilder builderOf(final User user) {
        return UserData.builder()
                .passwordHashes(toUserPasswordHashSet(user.getPasswordHashes()))
                .scopes(scopesMap(user));
    }

    private static Map<String, String> scopesMap(final User user) {
        final Map<?, ?> scopes = (Map<?, ?>) user.getScopes();
        if (scopes == null) {
            return emptyMap();
        } else {
            final Map<String, String> result = newHashMapWithExpectedSize(scopes.size());
            scopes.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
            return result;
        }
    }

    public static Set<UserPasswordHash> toUserPasswordHashSet(Collection<String> passwordHashes) {
        return passwordHashes.stream().map(UserPasswordHash::new).collect(toSet());
    }

    public static Set<UserPasswordHash> toUserPasswordHashSet(Collection<String> passwordHashes, String createdBy) {
        return passwordHashes.stream().map(h -> new UserPasswordHash(h, createdBy)).collect(toSet());
    }

}
