package org.zalando.planb.provider.realms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by hjacobs on 3/3/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstreamTokenResponse {
    @JsonProperty("uid")
    private String uid;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
