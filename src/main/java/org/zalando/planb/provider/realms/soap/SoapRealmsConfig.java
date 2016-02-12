package org.zalando.planb.provider.realms.soap;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ SoapRealmConfigurationProperties.class })
class SoapRealmsConfig {

    @Autowired
    private SoapRealmConfigurationProperties soapRealmConfigurationProperties;

    @Bean
    SoapRealms soapRealms() {
        SoapRealms realms = new SoapRealms();
        List<SoapRealmConfiguration> configs = soapRealmConfigurationProperties.getRealms();
        for (SoapRealmConfiguration config : configs) {
            SoapRealm realm = new SoapRealm(config);
            realms.add(realm);
        }
        return realms;
    }
}
