package org.zalando.planb.provider.realms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstreamTokenResponse {

    private String uid;

}
