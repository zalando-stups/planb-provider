package org.zalando.planb.provider;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationPropertiesBinding
public class StringToDurationConverter implements Converter<String, Duration> {

    @Override
    public Duration convert(String source) {
        return Duration.parse(source);
    }
}