package org.zalando.planb.provider;

import java.util.HashMap;
import java.util.Map;

public class TestRealm implements Realm {
    @Override
    public Map<String, Object> authenticate(final String user, final String password) throws AuthenticationFailedException {
        if (!password.equals("test")) {
            throw new Realm.AuthenticationFailedException();
        }

        return new HashMap<String, Object>() {{
            put("uid", user);
        }};
    }
}
