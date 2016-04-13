package exclude.from.component.scan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.zalando.planb.provider.ScopeProperties;
import org.zalando.planb.provider.ScopeService;

@Configuration
@Import(ScopeService.class)
@EnableConfigurationProperties(ScopeProperties.class)
public class ScopePropertiesTestConfig {
}
