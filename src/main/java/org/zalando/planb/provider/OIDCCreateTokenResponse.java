package org.zalando.planb.provider;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OIDCCreateTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("id_token")
    private String idToken;

    @JsonProperty("token_type")
    private String tokenType = "Bearer";

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("scope")
    private String scope;

    private OIDCCreateTokenResponse() {
    }

    public OIDCCreateTokenResponse(String accessToken, String idToken, long expiresIn, String scope) {
        this.accessToken = accessToken;
        this.idToken = idToken;
        this.expiresIn = expiresIn;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }
}
