package org.zalando.planb.provider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.planb.provider.realms.RealmPlugin;

@SpringApplicationConfiguration(classes = { Main.class })
@IntegrationTest
@ActiveProfiles("realms")
public class RealmPluginTest extends AbstractSpringTest {

    @Autowired
    private PluginRegistry<RealmPlugin, String> realmPluginRegistry;

    @Autowired
    private RealmConfig realmConfig;

    @Test
    public void testRealmPlugins() {

        // Cassandra-Realms
        RealmPlugin cassandraRealmPlugin_1 = realmPluginRegistry.getPluginFor("brot");
        Assertions.assertThat(cassandraRealmPlugin_1).isNotNull();
        Assertions.assertThat(cassandraRealmPlugin_1.getClass().getName()).contains("CassandraRealmPlugin");
        Realm cassandraRealm_1 = cassandraRealmPlugin_1.get("brot");
        Assertions.assertThat(cassandraRealm_1).isInstanceOf(Realm.class);
        Assertions.assertThat(cassandraRealm_1.getClass().getSimpleName()).contains("CassandraRealm");

        RealmPlugin cassandraRealmPlugin_2 = realmPluginRegistry.getPluginFor("zwiebel");
        Assertions.assertThat(cassandraRealmPlugin_2).isNotNull();
        Realm cassandraRealm_2 = cassandraRealmPlugin_2.get("zwiebel");
        Assertions.assertThat(cassandraRealm_2).isInstanceOf(Realm.class);
        Assertions.assertThat(cassandraRealm_2.getClass().getSimpleName()).contains("CassandraRealm");

        Assertions.assertThat(cassandraRealm_1).isNotEqualTo(cassandraRealm_2);

        Realm cassandraRealm_3 = realmConfig.get("brot");
        Assertions.assertThat(cassandraRealm_3).isInstanceOf(Realm.class);
        Assertions.assertThat(cassandraRealm_3.getClass().getSimpleName()).contains("CassandraRealm");

        Assertions.assertThat(cassandraRealm_3).isEqualTo(cassandraRealm_1);

        // Soap-Realms
        RealmPlugin soapRealmPlugin_1 = realmPluginRegistry.getPluginFor("zoap_1");
        Assertions.assertThat(soapRealmPlugin_1).isNotNull();
        Assertions.assertThat(soapRealmPlugin_1.getClass().getName()).contains("SoapRealmPlugin");
        Realm soapRealm_1 = soapRealmPlugin_1.get("zoap_1");
        Assertions.assertThat(soapRealm_1).isInstanceOf(Realm.class);
        Assertions.assertThat(soapRealm_1.getClass().getName()).contains("SoapRealm");

        RealmPlugin soapRealmPlugin_2 = realmPluginRegistry.getPluginFor("zoap_2");
        Assertions.assertThat(soapRealmPlugin_2).isNotNull();

        Realm soapRealm_2 = soapRealmPlugin_2.get("zoap_2");
        Assertions.assertThat(soapRealm_2).isInstanceOf(Realm.class);

        Assertions.assertThat(soapRealm_1).isNotEqualTo(soapRealm_2);

        Realm soapRealm_3 = realmConfig.get("zoap_2");
        Assertions.assertThat(soapRealm_3).isInstanceOf(Realm.class);
        Assertions.assertThat(soapRealm_3.getClass().getName()).contains("SoapRealm");

        Assertions.assertThat(soapRealm_3).isEqualTo(soapRealm_2);

        // choose default if not existent
        RealmPlugin defaultRealmPlugin = realmPluginRegistry.getPluginFor("notExistent_takeDefault",
                cassandraRealmPlugin_2);
        Assertions.assertThat(defaultRealmPlugin).isNotNull();
        Realm defaultRealm = defaultRealmPlugin.get("zwiebel");
        Assertions.assertThat(defaultRealm).isEqualTo(cassandraRealm_2);
    }

}
