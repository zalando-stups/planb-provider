package org.zalando.planb.provider;

import java.util.List;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

@Component
public class AlphaRealm implements RealmPlugin {

    private final List<String> supportedRealms = Lists.newArrayList("alpha");

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
