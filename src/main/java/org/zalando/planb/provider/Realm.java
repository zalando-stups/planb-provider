package org.zalando.planb.provider;

import com.google.common.collect.Lists;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

public interface Realm {

    static List<String> NEW_BCRYPT_SALT_PREFIXES = Lists.newArrayList("$2b", "$2y");
    static String OLD_BCRYPT_SALT_PREFIX = "$2a";

    static boolean checkBCryptPassword(String password, final String passwordHash) {
        String supportedHash;
        if (NEW_BCRYPT_SALT_PREFIXES.stream().anyMatch(prefix -> passwordHash.startsWith(prefix))) {
            // the stupid Spring BCrypt library only supports the original BCrypt salt revision
            // => simply adapt our hash to make Spring's BCrypt class happy..
            // see also https://github.com/zalando/planb-provider/issues/10
            supportedHash = OLD_BCRYPT_SALT_PREFIX + passwordHash.substring(OLD_BCRYPT_SALT_PREFIX.length());
        } else {
            supportedHash = passwordHash;
        }
        return BCrypt.checkpw(password, supportedHash);
    }


    default void initialize(String realmName) {
        // noop
    }

    String getName();
}
