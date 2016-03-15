package org.zalando.planb.provider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserPasswordHashTest {

    @Test
    public void testEquals() throws Exception {
        final UserPasswordHash instance1 = new UserPasswordHash();
        assertThat(new UserPasswordHash("foo", "me")).isEqualTo(new UserPasswordHash("foo", "someone-else"));
        assertThat(instance1).isNotEqualTo("instance-of-another-class");
        assertThat(instance1).isNotEqualTo(null);
        assertThat(instance1).isEqualTo(instance1);
    }
}
