package org.zalando.planb.provider;

import com.datastax.driver.mapping.annotations.Field;
import com.datastax.driver.mapping.annotations.UDT;

import static java.time.ZonedDateTime.now;

@UDT(keyspace = "provider", name = "user_password_hash")
public class UserPasswordHash {
    @Field(name = "password_hash")
    private String passwordHash;
    private int created;
    @Field(name = "created_by")
    private String createdBy;

    // default constructor for UDT mapping
    public UserPasswordHash() {}

    public UserPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserPasswordHash(String passwordHash, String createdBy) {
        this.passwordHash = passwordHash;
        this.created = (int) now().toEpochSecond();
        this.createdBy = createdBy;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
