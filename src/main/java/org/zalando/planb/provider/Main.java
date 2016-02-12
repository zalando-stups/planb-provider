package org.zalando.planb.provider;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(CassandraProperties.class)
public class Main {

    @Autowired
    private CassandraProperties cassandra;

    @Bean
    ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new SimpleModule().addSerializer(OIDCSigningKeysResponse.class, new OIDCSigningKeysSerializer()));
        return om;
    }

    @Bean
    Session initializeCassandra() {
        Cluster cluster = Cluster.builder()
                .addContactPoints(cassandra.getContactPoints())
                .withClusterName(cassandra.getClusterName())
                .withPort(cassandra.getPort())
                .build();

        return cluster.connect(cassandra.getKeyspace());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
