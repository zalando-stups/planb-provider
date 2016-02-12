package org.zalando.planb.provider.realms.cassandra;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.Lists;

@ConfigurationProperties(prefix = "planb.realms.cassandra")
class CassandraRealmConfigurationProperties {

    private List<CassandraRealmConfiguration> realms = Lists.newArrayList();

    public List<CassandraRealmConfiguration> getRealms() {
        return realms;
    }

    public void setRealms(List<CassandraRealmConfiguration> realms) {
        this.realms = realms;
    }

}
