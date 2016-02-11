package org.zalando.planb.provider;

import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zalando.planb.provider.domain.CustomerLoginResponse;

@Service("customerLoginClient")
public class CustomerLoginClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerLoginClient.class);

    public void authenticate() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress("http://localhost/ws/xxx?wsdl");
        factory.setServiceClass(CustomerLogin.class);


        CustomerLogin client = (CustomerLogin) factory.create();
        CustomerLoginResponse response = client.authenticate(1,"user","password");
        log.info("Response from server: {}", response);
    }
}
