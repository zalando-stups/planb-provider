package org.zalando.planb.provider.realms;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static org.springframework.web.util.UriComponentsBuilder.fromHttpUrl;
import static org.zalando.planb.provider.ScopeProperties.SPACE;

@Component
@Scope("prototype")
public class UpstreamUserRealm implements UserRealm {

    private UpstreamRealmProperties upstreamRealmProperties;
    private String realmName;

    private final RestTemplate rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    @Autowired
    public UpstreamUserRealm(UpstreamRealmProperties upstreamRealmProperties) {
        this.upstreamRealmProperties = upstreamRealmProperties;
    }

    String getAccessToken(String username, String password, Set<String> scopes) {
        final UriComponentsBuilder uriBuilder = fromHttpUrl(upstreamRealmProperties.getTokenServiceUrl());
        uriBuilder.queryParam("realm", realmName);
        if (scopes != null && !scopes.isEmpty()) {
            uriBuilder.queryParam("scope", scopes.stream().collect(joining(SPACE)));
        }

        final String basicAuth = Base64.getEncoder().encodeToString((username + ':' + password).getBytes(UTF_8));

        final RequestEntity<Void> tokenRequest = RequestEntity
                .get(uriBuilder.build().toUri())
                .header("Authorization", "Basic " + basicAuth).build();

        final ResponseEntity<String> tokenResponse;
        try {
            tokenResponse = rest.exchange(tokenRequest, String.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new UserRealmAuthenticationException(format("User %s login failed", username));
            } else {
                throw ex;
            }
        }

        return tokenResponse.getBody().replaceAll("\\s+", "");
    }

    @Override
    @HystrixCommand(ignoreExceptions = {RealmAuthenticationException.class})
    public Map<String, String> authenticate(String username, String password, Set<String> scopes, Set<String> defaultScopes) throws UserRealmAuthenticationException, UserRealmAuthorizationException {
        final String token = getAccessToken(username, password, scopes);

        UpstreamTokenResponse response = getTokenInfo(username, token);

        return singletonMap(SUB, response.getUid());
    }

    UpstreamTokenResponse getTokenInfo(String username, String token) {
        RequestEntity<Void> request = RequestEntity
                .get(URI.create(upstreamRealmProperties.getTokenInfoUrl()))
                .header("Authorization", "Bearer " + token).build();

        ResponseEntity<UpstreamTokenResponse> response;
        try {
            response = rest.exchange(request, UpstreamTokenResponse.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new UserRealmAuthenticationException(format("User %s login failed", username));
            } else {
                throw ex;
            }
        }
        return response.getBody();
    }

    @Override
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public String maskSubject(String sub) {
        return sub;
    }

}
