package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("it")
public class ExceptionHandlingIT extends AbstractOauthTest {

    @Test
    public void runtimeException() {
        RequestEntity request = RequestEntity.get(getUriWithPathAsUri("/throwError")).build();
        getRestTemplate().setErrorHandler(new PassThroughResponseErrorHandler());
        ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).contains("TEST_ERROR");
        assertThat(response.getBody()).doesNotContain("Caused by");
    }

    @Test
    public void methodNotAllowedException() {
        RequestEntity request = RequestEntity.get(getUriWithPathAsUri("/methodNotAllowed")).build();
        getRestTemplate().setErrorHandler(new PassThroughResponseErrorHandler());
        ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).contains("not supported");
        assertThat(response.getHeaders().get("Allow")).isNotEmpty();
        assertThat(response.getBody()).doesNotContain("Caused by");
    }

}
