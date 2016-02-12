package org.zalando.planb.provider;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.zalando.planb.provider.exception.AuthenticationFailedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringApplicationConfiguration(classes = {Main.class})
@WebIntegrationTest(randomPort = true)
@ActiveProfiles({"it","intern"})
public class CustomerLoginWebServiceRealmTest extends AbstractSpringTest {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginWebServiceRealmTest.class);

    public static final String UID = "uid";

    @Autowired
    private CustomerLoginRealm customerLoginRealm;

    @Value("${CUSTOMER_LOGIN_TEST_USER}")
    private String customerLoginTestUser;

    @Value("${CUSTOMER_LOGIN_TEST_PASSWORD}")
    private String customerLoginTestPassword;

    @Value("${CUSTOMER_LOGIN_TEST_CUSTOMER_NUMBER}")
    private String customerLoginTestCustomerNumber;

    @Test
    public void testAuthenticate() throws AuthenticationFailedException {
        Map<String, Object> authenticate = customerLoginRealm.authenticate(customerLoginTestUser, customerLoginTestPassword, new String[]{UID});
        assertThat(authenticate.get(UID)).isEqualTo(customerLoginTestCustomerNumber);
    }
}