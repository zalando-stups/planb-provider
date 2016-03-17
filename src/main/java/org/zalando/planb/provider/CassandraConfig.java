package org.zalando.planb.provider;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.EC2MultiRegionAddressTranslator;
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
        final Cluster.Builder builder = Cluster.builder();

        builder.addContactPoints(cassandra.getContactPoints().split(","));
        builder.withAddressTranslator(new EC2MultiRegionAddressTranslator());
        builder.withClusterName(cassandra.getClusterName());
        builder.withPort(cassandra.getPort());
        builder.build();

        if (cassandra.getUsername().isPresent() && cassandra.getPassword().isPresent()) {
            builder.withCredentials(cassandra.getUsername().get(), cassandra.getPassword().get());
        }

        return builder.build().connect(cassandra.getKeyspace());
    }
}
