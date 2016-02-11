package org.zalando.planb.provider;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Component
public class FirstRealm implements RealmPlugin {

    private final List<String> supportedRealms = Lists.newArrayList("first", "third");

    @Override
    public Map<String, Object> authenticate(String user, String password, String[] scopes)
            throws RealmAuthenticationFailedException {
        return Maps.newHashMap();
    }

    @Override
    public boolean supports(String delimiter) {
        return supportedRealms.contains(delimiter);
    }

}
