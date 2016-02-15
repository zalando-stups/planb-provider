package org.zalando.planb.provider;

import org.springframework.security.crypto.bcrypt.BCrypt;

public interface Realm {

    static boolean checkBCryptPassword(String password, String passwordHash) {
        return BCrypt.checkpw(password, passwordHash);
    }


    default void initialize(String realmName) {
        // noop
    }

    String getName();
}
