package org.zalando.planb.provider;

import java.net.URI;
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

    URI redirectUri;

    public AuthorizationCode(String code, String state, String clientId, String realm, Set<String> scopes, Map<String, String> claims, URI redirectUri) {
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

    public String getState() {
        return state;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRealm() {
        return realm;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Map<String, String> getClaims() {
        return claims;
    }

    public URI getRedirectUri() {
        return redirectUri;
    }
}
