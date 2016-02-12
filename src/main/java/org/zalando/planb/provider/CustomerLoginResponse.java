package org.zalando.planb.provider;

import javax.xml.bind.annotation.*;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class CustomerLoginResponse {

    @XmlElement(name = "customerNumber")
    protected String customerNumber;

    @XmlElement(name = "loginResult", required = true)
    protected String loginResult;

    @XmlAnyElement(lax = true)
    private List<Object> ignoredElements;

    public String getCustomerNumber() {
        return customerNumber;
    }


    public String getLoginResult() {
        return loginResult;
    }


    public List<Object> getIgnoredElements() {
        return ignoredElements;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("customerNumber", customerNumber)
                .add("loginResult", loginResult)
                .toString();
    }
}
