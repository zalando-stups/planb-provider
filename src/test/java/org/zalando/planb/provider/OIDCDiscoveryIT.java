package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("it")
public class OIDCDiscoveryIT extends AbstractOauthTest {

    @Test
    public void discoveryAvailability() {
        ResponseEntity<OIDCDiscoveryInformationResponse> response = getRestTemplate()
                .getForEntity(
                        URI.create(getOpenIdConfiguration()),
                        OIDCDiscoveryInformationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void jwksUrl() {
        ResponseEntity<OIDCDiscoveryInformationResponse> response = getRestTemplate()
                .getForEntity(
                        URI.create(getOpenIdConfiguration()),
                        OIDCDiscoveryInformationResponse.class);

        assertThat(response.getBody().getJwksUri()).isEqualTo(getHttpPublicKeysUri());
    }

    @Test
    public void jwksUrlProto() {
        RequestEntity request = RequestEntity.get(URI.create(getOpenIdConfiguration()))
                .header("X-Forwarded-Proto", "https")
                .build();

        ResponseEntity<OIDCDiscoveryInformationResponse> response = getRestTemplate()
                .exchange(request, OIDCDiscoveryInformationResponse.class);

        assertThat(response.getBody().getJwksUri()).isEqualTo(getHttpsPublicKeysUri());
    }

}
