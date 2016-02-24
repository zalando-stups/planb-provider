package org.zalando.planb.provider;

import org.junit.Test;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.zalando.planb.provider.CustomerUserRealm.maskEmail;

public class CustomerUserRealmTest {

    @Test
    public void testMaskEmail() throws Exception {
        final String masked1 = maskEmail("foo.bar@zalando.de");
        System.out.println(masked1);
        final String masked2 = maskEmail("foo.bar@zalando.de");
        System.out.println(masked2);
        final String masked3 = maskEmail("x@hello.world");
        System.out.println(masked3);

        // long emails will contain the first two letters in plaintext
        assertThat(masked1).startsWith("fo***@***.de");

        // the hash should be constant for same input (helps on debugging)
        assertThat(masked1).isEqualTo(masked2);

        // short emails will only contain the first letter in plaintext
        assertThat(masked3).startsWith("x***@***.world");

        // no email -> no masking
        assertThat(maskEmail("not-an-email")).isEqualTo("not-an-email");
    }
}
