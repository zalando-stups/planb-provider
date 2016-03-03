package org.zalando.planb.provider;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.bouncycastle.util.encoders.Hex.toHexString;
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
        MultiValueMap<String, Object> requestParameters = new LinkedMultiValueMap<>();
        requestParameters.add("realm", realmName);
        requestParameters.add("scope", scopes.stream().collect(joining(SPACE)));
        String basicAuth = Base64.getEncoder().encodeToString((username + ':' + password).getBytes(UTF_8));

        RequestEntity<Void> tokenRequest = RequestEntity
                .get(URI.create(upstreamRealmProperties.getTokenServiceUrl()))
                .header("Authorization", "Basic " + basicAuth).build();

        ResponseEntity<String> tokenResponse;
        try {
            tokenResponse = rest.exchange(tokenRequest, String.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new UserRealmAuthenticationException(format("User %s login failed", username));
            } else {
                throw ex;
            }
        }

        final String token = tokenResponse.getBody().replaceAll("\\s+", "");
        return token;
    }

    @Override
    @HystrixCommand(ignoreExceptions = {RealmAuthenticationException.class})
    public Map<String, String> authenticate(String username, String password, Set<String> scopes, Set<String> defaultScopes) throws UserRealmAuthenticationException, UserRealmAuthorizationException {
        final String token = getAccessToken(username, password, scopes);

        UpstreamTokenResponse response = getTokenInfo(username, token);

        return singletonMap(SUB, response.getUid());
    }

    private UpstreamTokenResponse getTokenInfo(String username, String token) {
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
