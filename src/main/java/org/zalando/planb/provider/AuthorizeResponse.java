package org.zalando.planb.provider;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthorizeResponse {

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_description")
    private String clientDescription;

    @JsonProperty("scopes")
    private Set<String> scopes;

    @JsonProperty("redirect")
    private String redirect;

    @JsonProperty("consent_needed")
    private boolean consentNeeded;

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
    private String username;

    @JsonIgnore
    private String password;

}
