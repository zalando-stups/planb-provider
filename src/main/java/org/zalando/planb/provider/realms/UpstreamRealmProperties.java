package org.zalando.planb.provider.realms;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "upstreamRealm")
public class UpstreamRealmProperties {

    private String tokenServiceUrl;

    private String tokenInfoUrl;

}
