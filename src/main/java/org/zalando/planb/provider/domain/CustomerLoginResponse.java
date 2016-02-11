package org.zalando.planb.provider.domain;

import com.google.common.base.MoreObjects;

public class CustomerLoginResponse {

    private String customerNumber;

    private String loginResult;

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
        return MoreObjects.toStringHelper(this)
                .add("customerNumber", customerNumber)
                .add("loginResult", loginResult)
                .toString();
    }
}
