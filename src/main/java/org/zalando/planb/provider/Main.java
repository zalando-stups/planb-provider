package org.zalando.planb.provider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.security.Security;

@Configuration
@EnableAutoConfiguration(exclude = {
        CassandraAutoConfiguration.class // TODO Disabled for now, because we have our own. Let's figure out, if Spring's config would also fit for us
})
@ComponentScan
@EnableHystrix
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class Main {

    static {
        // initialize BouncyCastle
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
        tomcat.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
        return tomcat;
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }
}
