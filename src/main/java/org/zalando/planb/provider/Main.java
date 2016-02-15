package org.zalando.planb.provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
@EnableAutoConfiguration(exclude = {
        CassandraAutoConfiguration.class // TODO Disabled for now, because we have our own. Let's figure out, if Spring's config would also fit for us
})
@ComponentScan // for IntelliJ
public class Main {

    static {
        // initialize BouncyCastle
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
