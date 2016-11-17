package org.zalando.planb.provider.realms;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class GuestCustomerResponse {

    @XmlElement(name = "loginResult", required = true)
    protected String loginResult;

    @XmlElement(name = "customerNumber")
    protected String customerNumber;

    @XmlAnyElement(lax = true)
    @SuppressWarnings("unused")
    private List<Object> ignoredElements;

    public String getLoginResult() {
        return loginResult;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }
}
