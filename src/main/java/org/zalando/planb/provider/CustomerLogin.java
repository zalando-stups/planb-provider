package org.zalando.planb.provider;

import org.zalando.planb.provider.domain.CustomerLoginResponse;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace = "", name = "CustomerLogin")
public interface CustomerLogin {

    @WebMethod
    @RequestWrapper(localName = "authenticate", targetNamespace = "")
    @ResponseWrapper(localName = "authenticateResponse", targetNamespace = "", className = "org.zalando.planb.provider.domain.CustomerLoginResponse")
    @WebResult(name = "return", targetNamespace = "")
    CustomerLoginResponse authenticate(
            @WebParam(name = "appDomainId", targetNamespace = "")
            int appDomainId,
            @WebParam(name = "email", targetNamespace = "")
            String email,
            @WebParam(name = "password", targetNamespace = "")
            String password);


}
