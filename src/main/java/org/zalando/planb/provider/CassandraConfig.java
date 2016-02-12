package org.zalando.planb.provider;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraConfig {

    @Autowired
    private CassandraProperties cassandra;

    @Bean
    Session initializeCassandra() {
        Cluster cluster = Cluster.builder()
                .addContactPoints(cassandra.getContactPoints())
                .withClusterName(cassandra.getClusterName())
                .withPort(cassandra.getPort())
                .build();

        return cluster.connect(cassandra.getKeyspace());
    }
}
