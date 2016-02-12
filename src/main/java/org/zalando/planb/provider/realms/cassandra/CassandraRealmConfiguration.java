package org.zalando.planb.provider.realms.cassandra;

import java.util.List;

public class CassandraRealmConfiguration {

    private String name;

    private List<String> seeds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSeeds() {
        return seeds;
    }

    public void setSeeds(List<String> seeds) {
        this.seeds = seeds;
    }

}