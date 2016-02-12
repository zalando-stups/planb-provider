package org.zalando.planb.provider.realms.soap;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

@ConfigurationProperties(prefix = "planb.realms.soap")
class SoapRealmConfigurationProperties {

    private List<SoapRealmConfiguration> realms = Lists.newArrayList();

    public List<SoapRealmConfiguration> getRealms() {
        return realms;
    }

    public void setRealms(List<SoapRealmConfiguration> realms) {
        this.realms = realms;
    }

}
