package org.zalando.planb.provider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.test.context.ActiveProfiles;

@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class RealmPluginTest extends AbstractSpringTest {

    @Autowired
    private PluginRegistry<RealmPlugin, String> realmPluginRegistry;

    @Test
    public void testRealmPlugins() {

        RealmPlugin alphaPlugin = realmPluginRegistry.getPluginFor("alpha");
        Assertions.assertThat(alphaPlugin).isNotNull();
        Assertions.assertThat(alphaPlugin).isInstanceOf(AlphaRealm.class);
        Assertions.assertThat(alphaPlugin).isInstanceOf(Realm.class);

        RealmPlugin firstRealm = realmPluginRegistry.getPluginFor("first");
        Assertions.assertThat(firstRealm).isNotNull();
        Assertions.assertThat(firstRealm).isInstanceOf(FirstRealm.class);
        Assertions.assertThat(firstRealm).isInstanceOf(Realm.class);

        RealmPlugin thirdRealm = realmPluginRegistry.getPluginFor("third");
        Assertions.assertThat(thirdRealm).isNotNull();
        Assertions.assertThat(thirdRealm).isInstanceOf(FirstRealm.class);
        Assertions.assertThat(thirdRealm).isInstanceOf(Realm.class);

        // null if no match
        RealmPlugin notExistent = realmPluginRegistry.getPluginFor("notexistent");
        Assertions.assertThat(notExistent).isNull();

        // choose default if not existent
        RealmPlugin defaultRealm = realmPluginRegistry.getPluginFor("notExistent_takeDefault", alphaPlugin);
        Assertions.assertThat(defaultRealm).isNotNull();
        Assertions.assertThat(defaultRealm).isInstanceOf(AlphaRealm.class);
        Assertions.assertThat(defaultRealm).isInstanceOf(Realm.class);
    }

}
