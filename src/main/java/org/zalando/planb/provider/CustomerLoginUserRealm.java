package org.zalando.planb.provider;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.zalando.stups.oauth2.jaxws.cxf.interceptors.OAuth2TokenInterceptor;
import org.zalando.stups.tokens.AccessTokens;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service("customerLoginClient")
public class CustomerLoginUserRealm implements UserRealm {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginUserRealm.class);

    public static final int APP_DOMAIN_ID = 1;
    public static final String SERVICE_ID = "customerLogin";

    @Value("${customerLoginRealm.url}")
    private String customerLoginRealmUrl;

    private final Environment environment;
    private final AccessTokens accessTokens;

    @Autowired
    public CustomerLoginUserRealm(Environment environment, AccessTokens accessTokens) {
        this.environment = environment;
        this.accessTokens = accessTokens;
    }

    private CustomerLoginWebService customerLoginWebService;

    @PostConstruct
    void initialize() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(customerLoginRealmUrl);
        factory.setServiceClass(CustomerLoginWebService.class);
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

        customerLoginWebService = (CustomerLoginWebService) factory.create();

    }

    @Override
    public Map<String, Object> authenticate(String user, String password, String[] scopes) throws RealmAuthenticationException {

        Optional<CustomerLoginResponse> response = ofNullable(customerLoginWebService.authenticate(APP_DOMAIN_ID, user, password));

        if (!response.isPresent() || !"SUCCESS".equals(response.get().getLoginResult())) {
            throw new RealmAuthenticationException("User not authenticated: " + user);
        }

        return new HashMap<String, Object>() {{
            put("uid", response.get().getCustomerNumber());
        }};
    }


}
