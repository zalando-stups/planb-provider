package org.zalando.planb.provider.realms;

import java.util.Map;
import java.util.Set;

public interface UserRealm extends Realm {

    Map<String, String> authenticate(String username, String password, Set<String> scopes, Set<String> defaultScopes)
            throws UserRealmAuthenticationException, UserRealmAuthorizationException;
}
