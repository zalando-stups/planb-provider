package org.zalando.planb.provider;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.zalando.stups.oauth2.jaxws.cxf.interceptors.OAuth2TokenInterceptor;
import org.zalando.stups.tokens.AccessTokens;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Component
@Scope("prototype")
public class CustomerUserRealm implements UserRealm {
    
    public static final int APP_DOMAIN_ID = 1;
    public static final String SERVICE_ID = "customerLogin";

    private final Environment environment;
    private final AccessTokens accessTokens;
    private final CustomerRealmProperties customerRealmProperties;
    private String realmName;

    @Autowired
    public CustomerUserRealm(Environment environment, AccessTokens accessTokens, CustomerRealmProperties customerRealmProperties) {
        this.environment = environment;
        this.accessTokens = accessTokens;
        this.customerRealmProperties = customerRealmProperties;
    }

    private CustomerRealmWebService customerRealmWebService;

    @PostConstruct
    void initialize() {
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

        customerRealmWebService = (CustomerRealmWebService) factory.create();

    }

    @Override
    public Map<String, Object> authenticate(String user, String password, String[] scopes) throws RealmAuthenticationException {

        Optional<CustomerResponse> response = ofNullable(customerRealmWebService.authenticate(APP_DOMAIN_ID, user, password));

        if (!response.isPresent() || !"SUCCESS".equals(response.get().getLoginResult())) {
            throw new RealmAuthenticationException(user, realmName);
        }

        return new HashMap<String, Object>() {{
            put("uid", response.get().getCustomerNumber());
        }};
    }


    @Override
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getName() {
        return realmName;
    }
}
