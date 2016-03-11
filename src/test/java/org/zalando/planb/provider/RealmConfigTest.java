package org.zalando.planb.provider;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.zalando.planb.provider.realms.*;

import java.util.Set;

import static org.assertj.core.api.StrictAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        RealmConfig config = new RealmConfig(null);
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
        RealmConfig config = new RealmConfig(null);
        config.getUserRealm("wrong-user-realm");
    }

    @Test(expected = RealmNotFoundException.class)
    public void testClientRealmNotFound() {
        RealmConfig config = new RealmConfig(null);
        config.getClientRealm("wrong-client-realm");
    }

    @Test
    public void testSetup() {
        RealmProperties props = new RealmProperties();
        props.getNames().add("/myrealm");
        props.getNames().add("/upstreamrealm");
        // NOTE: we leave out the leading slash to test the fallback
        props.getUserImpl().put("upstreamrealm", UpstreamUserRealm.class);
        RealmConfig config = new RealmConfig(props);
        BeanFactory mockBeanFactory = mock(BeanFactory.class);
        CassandraUserRealm mockUserRealm = mock(CassandraUserRealm.class);
        UpstreamUserRealm mockUpstreamUserRealm = mock(UpstreamUserRealm.class);
        CassandraClientRealm mockClientRealm = mock(CassandraClientRealm.class);
        when(mockBeanFactory.getBean(CassandraUserRealm.class)).thenReturn(mockUserRealm);
        when(mockBeanFactory.getBean(UpstreamUserRealm.class)).thenReturn(mockUpstreamUserRealm);
        when(mockBeanFactory.getBean(CassandraClientRealm.class)).thenReturn(mockClientRealm);
        config.setBeanFactory(mockBeanFactory);
        config.setup();
        assertThat(config.getUserRealm("/myrealm")).isSameAs(mockUserRealm);
        assertThat(config.getClientRealm("/myrealm")).isSameAs(mockClientRealm);
        assertThat(config.getUserRealm("/upstreamrealm")).isSameAs(mockUpstreamUserRealm);
        assertThat(config.getClientRealm("/upstreamrealm")).isSameAs(mockClientRealm);
    }
}
