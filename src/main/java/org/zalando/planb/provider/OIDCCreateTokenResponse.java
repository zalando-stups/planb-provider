package org.zalando.planb.provider;

public class OIDCCreateTokenResponse {
    private String access_token;
    private String id_token;
    private String token_type = "Bearer";
    private long expires_in;
    private String scope;


    public OIDCCreateTokenResponse(String access_token, String id_token, long expires_in, String scope) {
        this.access_token = access_token;
        this.id_token = id_token;
        this.expires_in = expires_in;
        this.scope = scope;
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getId_token() {
        return id_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public long getExpires_in() {
        return expires_in;
    }

    public String getScope() {
        return scope;
    }
}
