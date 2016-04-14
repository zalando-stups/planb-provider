package org.zalando.planb.provider.realms;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.zalando.planb.provider.realms.CustomerUserRealm.maskUsername;

public class CustomerUserRealmTest {

    @Test
    public void testMaskUsername() throws Exception {
        final String masked1 = maskUsername("foo.bar@zalando.de");
        System.out.println(masked1);
        final String masked2 = maskUsername("foo.bar@zalando.de");
        System.out.println(masked2);
        final String masked3 = maskUsername("x@hello.world");
        System.out.println(masked3);
        final String masked4 = maskUsername("");
        System.out.println(masked4);

        // long emails will contain the first two letters in plaintext
        assertThat(masked1).isEqualTo("fo***de (0e37cadb)");

        // the hash should be constant for same input (helps on debugging)
        assertThat(masked1).isEqualTo(masked2);

        assertThat(masked3).startsWith("x@***ld");

        assertThat(maskUsername("not-an-email")).startsWith("no***il (");

        // too short usernames (<4 chars) are not masked
        assertThat(maskUsername("123")).isEqualTo("123");
    }
}
