package org.zalando.planb.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@Getter
@AllArgsConstructor
@Builder
public class AuthorizationCode {

    private String code;

    private String state;

    private String clientId;

    private String realm;

    private Set<String> scopes;

    private Map<String, String> claims;

    private URI redirectUri;
}
