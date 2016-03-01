package org.zalando.planb.provider;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@Component
public class CurrentUserInSecurityContext implements CurrentUser {

    private static final String FORMAT = "%s/%s";
    private static final String REALM = "realm";
    private static final String UID = "uid";

    @Override
    public String get() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map(auth -> (OAuth2Authentication) auth)
                .map(OAuth2Authentication::getUserAuthentication)
                .map(Authentication::getDetails)
                .map(details -> (Map<?, ?>) details)
                .map(details -> format(FORMAT, details.get(REALM), details.get(UID)))
                .orElseThrow(() -> new IllegalStateException("No authentication found in SecurityContext"));
    }
}
