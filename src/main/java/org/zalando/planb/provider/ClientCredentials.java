package org.zalando.planb.provider;

import org.immutables.value.Value;

@Value.Immutable
public interface ClientCredentials {

    String clientId();

    String clientSecret();

}
