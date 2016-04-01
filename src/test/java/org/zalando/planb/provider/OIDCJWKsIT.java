package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("it")
public class OIDCJWKsIT extends AbstractOauthTest {

    @Test
    public void jwksResponse() {
        ResponseEntity<String> response = getRestTemplate().getForEntity(
                URI.create(getHttpPublicKeysUri()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);

        // TODO check the actual expected keys
    }
}
