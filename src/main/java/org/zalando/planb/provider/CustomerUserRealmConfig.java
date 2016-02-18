package org.zalando.planb.provider;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.zalando.stups.oauth2.jaxws.cxf.interceptors.OAuth2TokenInterceptor;
import org.zalando.stups.tokens.AccessTokens;

@Configuration
@EnableConfigurationProperties(CustomerRealmProperties.class)
public class CustomerUserRealmConfig {

    public static final String SERVICE_ID = "customerLogin";

    @Bean
    CustomerRealmWebService customerRealmWebService(Environment environment, AccessTokens accessTokens, CustomerRealmProperties customerRealmProperties) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(customerRealmProperties.getServiceUrl());
        factory.setServiceClass(CustomerRealmWebService.class);
        OAuth2TokenInterceptor oAuth2TokenInterceptor = new OAuth2TokenInterceptor(accessTokens, SERVICE_ID);
        factory.getOutInterceptors().add(oAuth2TokenInterceptor);

        if (environment.containsProperty("debug") || environment.containsProperty("trace")) {
            LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
            loggingInInterceptor.setPrettyLogging(true);
            LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
            loggingOutInterceptor.setPrettyLogging(true);
            factory.getInInterceptors().add(loggingInInterceptor);
            factory.getOutInterceptors().add(loggingOutInterceptor);
        }

        return (CustomerRealmWebService) factory.create();

    }

}
