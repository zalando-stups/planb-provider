package org.zalando.planb.provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.zalando.planb.provider.realms.UpstreamRealmProperties;

import java.security.Security;

@Configuration
@EnableAutoConfiguration(exclude = {
        CassandraAutoConfiguration.class // TODO Disabled because we have our own config
})
@ComponentScan
@EnableHystrix
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(value = {ScopeProperties.class, UpstreamRealmProperties.class})
public class Main {

    static {
        // initialize BouncyCastle
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
