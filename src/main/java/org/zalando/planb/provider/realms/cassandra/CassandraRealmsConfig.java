package org.zalando.planb.provider.realms.cassandra;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ CassandraRealmConfigurationProperties.class })
class CassandraRealmsConfig {

    @Autowired
    private CassandraRealmConfigurationProperties realmsConfigurationProperties;

    // more autowired deps possible to inject by hand into cassandra-realm

    @Bean
    CassandraRealms cassandraRealms() {
        CassandraRealms realms = new CassandraRealms();
        List<CassandraRealmConfiguration> configs = realmsConfigurationProperties.getRealms();
        for (CassandraRealmConfiguration config : configs) {

            CassandraRealm realm = new CassandraRealm(config);
            realms.add(realm);
        }
        return realms;
    }

}
