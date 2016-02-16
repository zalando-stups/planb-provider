package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.zalando.stups.oauth2.spring.security.expression.ExtendedOAuth2WebSecurityExpressionHandler;
import org.zalando.stups.oauth2.spring.server.TokenInfoResourceServerTokenServices;

import static org.springframework.boot.actuate.autoconfigure.ManagementServerProperties.ACCESS_OVERRIDE_ORDER;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableResourceServer
@Order(ACCESS_OVERRIDE_ORDER)
@EnableConfigurationProperties(SecurityConfig.ApiSecurityProperties.class)
public class SecurityConfig extends ResourceServerConfigurerAdapter {

    @Autowired
    private ApiSecurityProperties apiSecurity;

    @Autowired
    private ResourceServerProperties resourceServerProperties;

    @Bean
    public ResourceServerTokenServices tokeninfoTokenServices() {
        return new TokenInfoResourceServerTokenServices(resourceServerProperties.getTokenInfoUri());
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        // add support for #oauth2.hasRealm() expressions
        resources
                .resourceId("planb")
                .expressionHandler(new ExtendedOAuth2WebSecurityExpressionHandler());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .sessionManagement().sessionCreationPolicy(STATELESS)

                .and().authorizeRequests()

                .antMatchers("/raw-sync/**").access(apiSecurity.getRawSyncExpr());
    }


    @ConfigurationProperties(prefix = "api.security")
    static class ApiSecurityProperties {

        private String rawSyncExpr = "#oauth2.hasScope('uid')";

        public String getRawSyncExpr() {
            return rawSyncExpr;
        }

        public void setRawSyncExpr(String rawSyncExpr) {
            this.rawSyncExpr = rawSyncExpr;
        }
    }
}
