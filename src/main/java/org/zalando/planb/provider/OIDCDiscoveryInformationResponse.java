package org.zalando.planb.provider;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OIDCDiscoveryInformationResponse {

    public static final String KEYS_PATH = "/oauth2/connect/keys";

    private final String issuer = "PlanB";

    @JsonProperty("jwks_uri")
    private String jwksUri;

    protected OIDCDiscoveryInformationResponse() {
    }

    public OIDCDiscoveryInformationResponse(String proto, String hostname) {
        jwksUri = proto + "://" + hostname + KEYS_PATH;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }
}
