package org.zalando.planb.provider.realms;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upstreamRealm")
public class UpstreamRealmProperties {

    private String tokenServiceUrl;

    private String tokenInfoUrl;

    public String getTokenServiceUrl() {
        return tokenServiceUrl;
    }

    public void setTokenServiceUrl(String tokenServiceUrl) {
        this.tokenServiceUrl = tokenServiceUrl;
    }

    public String getTokenInfoUrl() {
        return tokenInfoUrl;
    }

    public void setTokenInfoUrl(String tokenInfoUrl) {
        this.tokenInfoUrl = tokenInfoUrl;
    }
}
