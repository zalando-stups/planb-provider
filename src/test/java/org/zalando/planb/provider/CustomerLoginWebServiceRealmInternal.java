package org.zalando.planb.provider;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles({"it"})
public class CustomerLoginWebServiceRealmInternal extends AbstractSpringTest {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginWebServiceRealmInternal.class);

    public static final String UID = "uid";

    @Autowired
    private CustomerLoginRealm customerLoginRealm;

    @Value("${customerLoginRealm.testUser}")
    private String customerLoginTestUser;

    @Value("${customerLoginRealm.testPassword}")
    private String customerLoginTestPassword;

    @Value("${customerLoginRealm.testCustomerNumber}")
    private String customerLoginTestCustomerNumber;

    @Test
    public void testAuthenticate() throws RealmAuthenticationFailedException {
        Map<String, Object> authenticate = customerLoginRealm.authenticate(customerLoginTestUser, customerLoginTestPassword, new String[]{UID});
        assertThat(authenticate.get(UID)).isEqualTo(customerLoginTestCustomerNumber);
    }
}