package org.zalando.planb.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ClientCredentials {

    private String clientId;
    private String clientSecret;

}
