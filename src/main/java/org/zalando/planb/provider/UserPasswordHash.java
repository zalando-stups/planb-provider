package org.zalando.planb.provider;

import com.datastax.driver.mapping.annotations.Field;
import com.datastax.driver.mapping.annotations.UDT;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static java.time.ZonedDateTime.now;

@NoArgsConstructor
@EqualsAndHashCode(exclude={"created","createdBy"})
@UDT(keyspace = "provider", name = "user_password_hash")
public class UserPasswordHash {
    @Getter @Setter
    @Field(name = "password_hash")
    private String passwordHash;
    @Getter @Setter
    private int created;
    @Getter @Setter
    @Field(name = "created_by")
    private String createdBy;

    public UserPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserPasswordHash(String passwordHash, String createdBy) {
        this.passwordHash = passwordHash;
        this.created = (int) now().toEpochSecond();
        this.createdBy = createdBy;
    }
}
