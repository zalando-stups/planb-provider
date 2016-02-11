package org.zalando.planb.provider;

import org.zalando.planb.provider.exception.AuthenticationFailedException;

import java.util.Map;

public interface Realm {

    Map<String,Object> authenticate(String user, String password) throws AuthenticationFailedException;

}
