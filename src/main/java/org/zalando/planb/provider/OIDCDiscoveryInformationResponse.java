package org.zalando.planb.provider;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OIDCDiscoveryInformationResponse {
    private final String issuer = "PlanB";

    @JsonProperty("jwks_uri")
    private String jwksUri;

    public OIDCDiscoveryInformationResponse(String proto, String hostname) {
        jwksUri = proto + "://" + hostname + "/oauth2/v3/certs";
    }

    public String getIssuer() {
        return issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }
}
