package org.zalando.planb.provider;

import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@ActiveProfiles("scopePropertiesTest") // load a specific config file
public class ScopePropertiesTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private ScopeService scopeService;

    @BeforeClass
    public static void setUpSystemProperties() {
        // override services realm with system property
        System.setProperty("scope.defaults.services", "id team");
        System.setProperty("SCOPE_DEFAULTS_FOOBAR", "id team");
    }

    @AfterClass
    public static void resetSystemProperties() {
        System.clearProperty("scope.defaults.services");
    }

    @Test
    public void testDefaultScopeByRealm() throws Exception {
        assertThat(scopeService.getDefaultScopesByRealm("unknown-realm")).isEmpty();
        assertThat(scopeService.getDefaultScopesByRealm("foo")).containsOnly("bar", "name");
        assertThat(scopeService.getDefaultScopesByRealm("hello")).isEmpty();
        assertThat(scopeService.getDefaultScopesByRealm("/customers")).containsOnly("email");
        assertThat(scopeService.getDefaultScopesByRealm("customers")).containsOnly("email");
        assertThat(scopeService.getDefaultScopesByRealm("services")).containsOnly("id", "team");
        assertThat(scopeService.getDefaultScopesByRealm("foobar")).containsOnly("id", "team");
        assertThat(scopeService.getDefaultScopesByRealm("Foobar")).containsOnly("id", "team");
        assertThat(scopeService.getDefaultScopesByRealm("fooBar")).containsOnly("id", "team");
    }

    @Configuration
    @Import(ScopeService.class)
    @EnableConfigurationProperties(ScopeProperties.class)
    static class TestConfig {
    }
}
