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
import org.zalando.planb.provider.exception.AuthenticationFailedException;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service("customerLoginClient")
public class CustomerLoginRealm implements Realm {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginRealm.class);
    public static final int APP_DOMAIN_ID = 1;

    @Value("${customerLoginRealm.url}")
    private String customerLoginRealmUrl;

    private final Environment environment;

    @Autowired
    public CustomerLoginRealm(Environment environment) {
        this.environment = environment;
    }

    private CustomerLoginWebService customerLoginWebService;

    @PostConstruct
    void initialize() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(customerLoginRealmUrl);
        factory.setServiceClass(CustomerLoginWebService.class);

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
    public Map<String, Object> authenticate(String user, String password, String[] scopes) throws AuthenticationFailedException {

        Optional<CustomerLoginResponse> response = ofNullable(customerLoginWebService.authenticate(APP_DOMAIN_ID, user, password));

        if (!response.isPresent() || !"SUCCESS".equals(response.get().getLoginResult())) {
            throw new AuthenticationFailedException("User not authenticated: " + user);
        }

        return new HashMap<String, Object>() {{
            put("uid", response.get().getCustomerNumber());
        }};
    }


}
