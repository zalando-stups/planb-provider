package org.zalando.planb.provider.config;

import java.util.Map;

import org.assertj.core.util.Maps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.planb.provider.Realm;

@Configuration
public class PlanBProviderConfig {

    @Bean
    public Realm realm() {
        return new Realm() {
            @Override
            public Map<String, Object> authenticate(String user, String password) throws AuthenticationFailedException {
                return Maps.newHashMap();
            }
        };
    }
}
