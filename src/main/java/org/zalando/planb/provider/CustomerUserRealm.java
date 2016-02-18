package org.zalando.planb.provider;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

@Component
@Scope("prototype")
public class CustomerUserRealm implements UserRealm {

    public static final int APP_DOMAIN_ID = 1;
    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String UID = "uid";

    private CustomerRealmWebService customerRealmWebService;
    private String realmName;

    @Autowired
    public CustomerUserRealm(CustomerRealmWebService customerRealmWebService) {
        this.customerRealmWebService = customerRealmWebService;
    }

    @Override
    @HystrixCommand(ignoreExceptions = {RealmAuthenticationException.class})
    public Map<String, Object> authenticate(String user, String password, String[] scopes) throws RealmAuthenticationException {

        if (user == null || password == null || user.trim().isEmpty() || password.trim().isEmpty()) {
            throw new RealmAuthenticationException(user, realmName);
        }

        final CustomerResponse response = ofNullable(customerRealmWebService.authenticate(APP_DOMAIN_ID, user, password))
                .filter(r -> SUCCESS_STATUS.equals(r.getLoginResult()))
                .orElseThrow(() -> new RealmAuthenticationException(user, realmName));

        return singletonMap(UID, response.getCustomerNumber());
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
