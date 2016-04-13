package org.zalando.planb.provider;

import org.immutables.value.Value;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@Value.Immutable
public interface AuthorizationCode {

    String code();

    String state();

    String clientId();

    String realm();

    Set<String> scopes();

    Map<String, String> claims();

    URI redirectUri();
}
