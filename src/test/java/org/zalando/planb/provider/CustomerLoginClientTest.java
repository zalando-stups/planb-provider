package org.zalando.planb.provider;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;

@SpringApplicationConfiguration(classes = { Main.class })
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("it")
public class CustomerLoginClientTest extends AbstractSpringTest {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginClientTest.class);

    @Autowired
    private CustomerLoginClient customerLoginClient;

    @Test
    public void testAuthenticate() throws Exception {
        customerLoginClient.authenticate();
    }
}