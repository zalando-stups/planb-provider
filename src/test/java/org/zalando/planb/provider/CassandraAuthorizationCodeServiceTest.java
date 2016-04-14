package org.zalando.planb.provider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CassandraAuthorizationCodeServiceTest {

    @Test
    public void randomCode() {
        String code1 = CassandraAuthorizationCodeService.getRandomCode();
        assertThat(code1).matches("^[a-zA-Z0-9_-]+$");
        assertThat(code1).hasSize(32);

        String code2 = CassandraAuthorizationCodeService.getRandomCode();
        assertThat(code2).matches("^[a-zA-Z0-9_-]+$");
        assertThat(code2).hasSize(32);
        assertThat(code2).isNotEqualTo(code1);
    }
}
