package org.zalando.planb.provider;

import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
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
    private ScopeProperties scopeProperties;

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
    public void testDefaultScopes() throws Exception {
        assertThat(scopeProperties.getDefaultScopes("unknown-realm")).isEmpty();
        assertThat(scopeProperties.getDefaultScopes("foo")).containsOnly("bar", "name");
        assertThat(scopeProperties.getDefaultScopes("hello")).isEmpty();
        assertThat(scopeProperties.getDefaultScopes("/customers")).containsOnly("email");
        assertThat(scopeProperties.getDefaultScopes("customers")).containsOnly("email");
        assertThat(scopeProperties.getDefaultScopes("services")).containsOnly("id", "team");
        assertThat(scopeProperties.getDefaultScopes("foobar")).containsOnly("id", "team");
    }

    @Configuration
    @EnableConfigurationProperties(ScopeProperties.class)
    static class TestConfig {
    }
}
