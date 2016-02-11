package org.zalando.planb.provider.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.planb.provider.Realm;
import org.zalando.planb.provider.TestRealm;

@Configuration
public class PlanBProviderConfig {

    @Bean
    public Realm realm() {
        return new TestRealm();
    }
}
