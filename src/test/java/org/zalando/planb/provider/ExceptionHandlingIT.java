package org.zalando.planb.provider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("it")
public class ExceptionHandlingIT extends AbstractOauthTest {

    @Test
    public void error() {
        RequestEntity request = RequestEntity.get(getUriWithPathAsUri("/throwError")).build();
        getRestTemplate().setErrorHandler(new PassThroughResponseErrorHandler());
        ResponseEntity<String> response = getRestTemplate().exchange(request, String.class);
        Assertions.assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        Assertions.assertThat(response.getBody()).contains("TEST_ERROR");
        Assertions.assertThat(response.getBody()).doesNotContain("Caused by");
    }

}
