package org.zalando.planb.provider;

import java.util.Map;

public interface UserRealm {

    Map<String,Object> authenticate(String user, String password, String[] scopes) throws RealmAuthenticationFailedException;

}
