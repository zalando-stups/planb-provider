package org.zalando.planb.provider;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthorizeResponse {

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_description")
    private String clientDescription;

    @JsonProperty("scopes")
    private Set<String> scopes;

    @JsonProperty("redirect")
    private String redirect;

    @JsonIgnore
    private String responseType;

    @JsonIgnore
    private String scope;

    @JsonIgnore
    private String realm;

    @JsonIgnore
    private String state;

    @JsonIgnore
    private String clientId;

    @JsonIgnore
    private boolean isConsentNeeded;

    @JsonIgnore
    private String username;

    @JsonIgnore
    private String password;

}
