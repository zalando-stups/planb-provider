package org.zalando.planb.provider.domain;

public class CustomerLoginRequest {

    private int appDomainId;

    private String email;

    private String password;

    public int getAppDomainId() {
        return appDomainId;
    }

    public void setAppDomainId(int appDomainId) {
        this.appDomainId = appDomainId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
