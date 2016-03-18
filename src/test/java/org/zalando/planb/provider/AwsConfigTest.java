package org.zalando.planb.provider;

import com.datastax.driver.core.policies.AddressTranslator;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = AwsConfig.class)
@ActiveProfiles("aws")
public class AwsConfigTest extends AbstractSpringTest {

    @Autowired
    private Optional<AddressTranslator> addressTranslator;

    @Test
    public void testAddressTranslator() throws Exception {
        assertThat(addressTranslator).isPresent();
    }
}
