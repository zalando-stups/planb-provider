package org.zalando.planb.provider;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace = "http://service.webservice.customer.zalando.de/", name = "CustomerLoginWebService")
public interface CustomerRealmWebService {

    @HystrixCommand
    @WebMethod(operationName = "authenticate")
    @ResponseWrapper(localName = "authenticateResponse")
    CustomerResponse authenticate(
            @WebParam(name = "appDomainId")
            int appDomainId,
            @WebParam(name = "email")
            String email,
            @WebParam(name = "password")
            String password);
}
