package org.zalando.planb.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {

    @Bean
    ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new SimpleModule().addSerializer(OIDCSigningKeysResponse.class, new OIDCSigningKeysSerializer()));
        return om;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
