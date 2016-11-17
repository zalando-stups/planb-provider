package org.zalando.planb.provider.realms;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static org.zalando.planb.provider.realms.CustomerUserRealm.SUCCESS_STATUS;
import static org.zalando.planb.provider.realms.CustomerUserRealm.maskUsername;

@Component
@Scope("prototype")
public class GuestCustomerUserRealm implements UserRealm {

    private CustomerRealmWebService customerRealmWebService;
    private String realmName;

    @Autowired
    public GuestCustomerUserRealm(CustomerRealmWebService customerRealmWebService) {
        this.customerRealmWebService = customerRealmWebService;
    }

    @Override
    @HystrixCommand(ignoreExceptions = {RealmAuthenticationException.class})
    public Map<String, String> authenticate(final String username,
                                            final String password,
                                            final Set<String> scopes,
                                            final Set<String> defaultScopes)
            throws  UserRealmAuthenticationException, UserRealmAuthorizationException {

        final GuestCustomerResponse response = ofNullable(customerRealmWebService.authenticateGuest(username, password))
                .filter(r -> SUCCESS_STATUS.equals(r.getLoginResult()))
                .orElseThrow(() ->  new UserRealmAuthenticationException(format("Guest Customer %s login failed",
                        maskUsername(username))));

        return singletonMap(SUB, response.getCustomerNumber());
    }

    @Override
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public String getName() {
        return realmName;
    }

    @Override
    public String maskSubject(String sub) {
        return maskUsername(sub);
    }
}
