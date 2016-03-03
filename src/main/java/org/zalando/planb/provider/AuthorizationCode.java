package org.zalando.planb.provider;

import java.util.Map;
import java.util.Set;

/**
 * Created by hjacobs on 3/3/16.
 */
public class AuthorizationCode {

    String code;

    String state;

    String clientId;

    String realm;

    Set<String> scopes;

    Map<String, String> claims;

    String redirectUri;

    public AuthorizationCode(String code, String state, String clientId, String realm, Set<String> scopes, Map<String, String> claims, String redirectUri) {
        this.code = code;
        this.state = state;
        this.clientId = clientId;
        this.realm = realm;
        this.scopes = scopes;
        this.claims = claims;
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public Map<String, String> getClaims() {
        return claims;
    }

    public String getRedirectUri() {
        return redirectUri;
    }
}
