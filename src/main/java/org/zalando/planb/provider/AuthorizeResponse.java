package org.zalando.planb.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizeResponse {

    @JsonProperty("client_name")
    String clientName;
    @JsonProperty("client_description")
    String clientDescription;
    @JsonProperty("scopes")
    Set<String> scopes;
    @JsonProperty("redirect")
    String redirect;

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public String getRedirect() {
        return redirect;
    }

    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientDescription() {
        return clientDescription;
    }

    public void setClientDescription(String clientDescription) {
        this.clientDescription = clientDescription;
    }
}
