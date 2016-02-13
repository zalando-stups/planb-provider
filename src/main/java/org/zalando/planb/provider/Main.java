package org.zalando.planb.provider;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = {
        CassandraAutoConfiguration.class // TODO Disabled for now, because we have our own. Let's figure out, if Spring's config would also fit for us
})
@ComponentScan // for IntelliJ
public class Main {

    @Bean
    Module oidcSigningKeysResponseModule() {
        return new SimpleModule().addSerializer(OIDCSigningKeysResponse.class, new OIDCSigningKeysSerializer());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
