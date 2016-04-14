package exclude.from.component.scan;

import com.datastax.driver.core.policies.AddressTranslator;
import com.datastax.driver.core.policies.IdentityTranslator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraTestAddressTranslatorConfig {

    @Bean
    AddressTranslator addressTranslator() {
        return new IdentityTranslator();
    }

}
