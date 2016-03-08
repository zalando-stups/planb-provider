package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.zalando.planb.provider.RealmConfig.ensureLeadingSlash;
import static org.zalando.planb.provider.RealmConfig.stripLeadingSlash;

public class RealmConfigTest {

    @Test
    public void testEnsureLeadingSlashWhenMissing() throws Exception {
        assertThat(ensureLeadingSlash("foo")).isEqualTo("/foo");
    }

    @Test
    public void testEnsureLeadingSlashWhenPresent() throws Exception {
        assertThat(ensureLeadingSlash("/foo")).isEqualTo("/foo");
    }

    @Test
    public void testStripLeadingSlash() {
        assertThat(stripLeadingSlash("foo")).isEqualTo("foo");
        assertThat(stripLeadingSlash("/foo")).isEqualTo("foo");
    }

    @Test
    public void testFindRealmInHostEmpty() {
        RealmConfig config = new RealmConfig();
        assertThat(config.findRealmNameInHost("foo")).isEmpty();
    }

    @Test
    public void testFindRealmInHost() {
        Set<String> realmNames = ImmutableSet.of("/foo", "/bar");
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "somethingelse")).isEmpty();
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "foobar")).isEmpty();
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "xfoox")).isEmpty();
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "provider.foo.example.org")).contains("/foo");
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "provider-bar.example.org")).contains("/bar");
        // we sort the realm names, so it will return the first one if both match
        assertThat(RealmConfig.findRealmNameInHost(realmNames, "foo.bar")).contains("/bar");
    }

    @Test(expected = RealmNotFoundException.class)
    public void testUserRealmNotFound() {
        RealmConfig config = new RealmConfig();
        config.getUserRealm("wrong-user-realm");
    }

    @Test(expected = RealmNotFoundException.class)
    public void testClientRealmNotFound() {
        RealmConfig config = new RealmConfig();
        config.getUserRealm("wrong-client-realm");
    }
}
