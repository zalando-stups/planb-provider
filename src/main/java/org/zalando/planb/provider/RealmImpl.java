package org.zalando.planb.provider;

import org.springframework.stereotype.Service;
import org.zalando.planb.provider.exception.AuthenticationFailedException;

import java.util.Map;

@Service("realm")
public class RealmImpl implements Realm {
    @Override
    public Map<String, Object> authenticate(String user, String password) throws AuthenticationFailedException {
        throw new AuthenticationFailedException("Not implemented yet. :/");
    }
}
