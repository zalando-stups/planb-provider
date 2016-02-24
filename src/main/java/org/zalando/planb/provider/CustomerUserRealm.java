package org.zalando.planb.provider;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static org.bouncycastle.util.encoders.Hex.toHexString;

@Component
@Scope("prototype")
public class CustomerUserRealm implements UserRealm {

    public static final int APP_DOMAIN_ID = 1;
    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String UID = "uid";

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^(..).*(..)$");
    private static final String USERNAME_MASK = "$1***$2";

    private CustomerRealmWebService customerRealmWebService;
    private String realmName;

    @Autowired
    public CustomerUserRealm(CustomerRealmWebService customerRealmWebService) {
        this.customerRealmWebService = customerRealmWebService;
    }

    @Override
    @HystrixCommand(ignoreExceptions = {RealmAuthenticationException.class})
    public Map<String, Object> authenticate(String username, String password, String[] scopes) throws RealmAuthenticationException {
        final CustomerResponse response = ofNullable(customerRealmWebService.authenticate(APP_DOMAIN_ID, username, password))
                .filter(r -> SUCCESS_STATUS.equals(r.getLoginResult()))
                .orElseThrow(() -> new UserRealmAuthenticationException(maskUsername(username), realmName));

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

    @VisibleForTesting
    static String maskUsername(String username) {
        final Matcher matcher = USERNAME_PATTERN.matcher(username);
        if (matcher.matches()) {
            final SHA256.Digest digest = new SHA256.Digest();
            digest.update(username.getBytes());
            return matcher.replaceAll(USERNAME_MASK) + " (" + toHexString(digest.digest()).substring(0, 8) + ")";
        } else {
            return username;
        }
    }
}
