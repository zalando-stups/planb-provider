package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.planb.provider.realms.ClientRealm;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@Component
public class ScopeService {

    private static final String SPACE = " ";

    @Autowired
    private ScopeProperties scopeProperties;

    public Set<String> getDefaultScopesForClient(final ClientRealm clientRealm, final ClientData clientData) {
        return clientData.getDefaultScopes().isEmpty() ?
                getDefaultScopesByRealm(clientRealm.getName()) : clientData.getDefaultScopes();
    }

    public Set<String> getDefaultScopesForClient(final ClientRealm clientRealm, final String clientId) {
        return clientRealm
                .get(clientId)
                .map(clientData -> getDefaultScopesForClient(clientRealm, clientData))
                .orElse(getDefaultScopesByRealm(clientRealm.getName()));
    }

    public Set<String> getDefaultScopesForClient(final ClientRealm clientRealm, final Optional<String> clientId) {
        return clientId
                .map((theClientId) -> getDefaultScopesForClient(clientRealm, theClientId))
                .orElse(getDefaultScopesByRealm(clientRealm.getName()));
    }

    public Set<String> getDefaultScopesByRealm(String realm) {
        return split(Optional.ofNullable(realm)
                .map(RealmConfig::stripLeadingSlash)
                .map(scopeProperties.getDefaults()::get));
    }

    public static String get(Optional<String> scope) {
        return join(split(scope));
    }

    public static Set<String> split(String scope) {
        return ImmutableSet.copyOf(scope.split(SPACE)).stream().sorted().collect(Collectors.toSet());
    }

    public static Set<String> split(Optional<String> scope) {
        return scope.map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ScopeService::split)
                .orElseGet(Collections::emptySet);
    }

    public static String join(Set<String> scopes) {
        return scopes.stream().sorted().collect(joining(SPACE));
    }

}
