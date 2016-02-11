package org.zalando.planb.provider;

import java.util.Map;

public interface Realm {

    Map<String,Object> authenticate(String user, String password) throws AuthenticationFailedException;

    class AuthenticationFailedException extends Exception {}
}
