package org.zalando.planb.provider;

import org.junit.Test;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.zalando.planb.provider.RealmConfig.ensureLeadingSlash;

public class RealmConfigTest {

    @Test
    public void testEnsureLeadingSlashWhenMissing() throws Exception {
        assertThat(ensureLeadingSlash("foo")).isEqualTo("/foo");
    }

    @Test
    public void testEnsureLeadingSlashWhenPresent() throws Exception {
        assertThat(ensureLeadingSlash("/foo")).isEqualTo("/foo");
    }
}
