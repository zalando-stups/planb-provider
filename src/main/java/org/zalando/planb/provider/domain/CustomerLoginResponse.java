package org.zalando.planb.provider.domain;

import javax.xml.bind.annotation.*;

import static com.google.common.base.MoreObjects.toStringHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "customerLoginResponse", propOrder = {
        "customerNumber","loginResult"
})
@XmlRootElement(name="return")
public class CustomerLoginResponse {

    @XmlElement(name = "customerNumber")
    protected String customerNumber;

    @XmlElement(name = "loginResult")
    protected String loginResult;

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(String loginResult) {
        this.loginResult = loginResult;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("customerNumber", customerNumber)
                .add("loginResult", loginResult)
                .toString();
    }
}
