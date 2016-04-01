package org.zalando.planb.provider.realms;

import javax.xml.bind.annotation.*;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class CustomerResponse {

    @XmlElement(name = "customerNumber")
    protected String customerNumber;

    @XmlElement(name = "loginResult", required = true)
    protected String loginResult;

    @XmlAnyElement(lax = true)
    @SuppressWarnings("unused")
    private List<Object> ignoredElements;

    public String getCustomerNumber() {
        return customerNumber;
    }


    public String getLoginResult() {
        return loginResult;
    }

}
