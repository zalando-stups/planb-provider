package org.zalando.planb.provider;

import java.util.Map;

public interface ClientRealm {

    Map<String,Object> authenticate(String clientId, String clientSecret, String[] scopes) throws RealmAuthenticationFailedException;

}
