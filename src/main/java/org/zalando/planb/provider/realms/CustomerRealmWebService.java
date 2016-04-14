package org.zalando.planb.provider.realms;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace = "http://service.webservice.customer.zalando.de/", name = "CustomerLoginWebService")
public interface CustomerRealmWebService {

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
