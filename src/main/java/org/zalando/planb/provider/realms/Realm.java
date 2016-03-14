package org.zalando.planb.provider.realms;

import com.google.common.collect.Lists;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

public interface Realm {

    String SUB = "sub";

    List<String> NEW_BCRYPT_SALT_PREFIXES = Lists.newArrayList("$2b", "$2y");
    String OLD_BCRYPT_SALT_PREFIX = "$2a";

    static boolean checkBCryptPassword(final String password, final String passwordHash) {
        String supportedHash;
        if (NEW_BCRYPT_SALT_PREFIXES.stream().anyMatch(passwordHash::startsWith)) {
            // the stupid Spring BCrypt library only supports the original BCrypt salt revision
            // => simply adapt our hash to make Spring's BCrypt class happy..
            // see also https://github.com/zalando/planb-provider/issues/10
            supportedHash = OLD_BCRYPT_SALT_PREFIX + passwordHash.substring(OLD_BCRYPT_SALT_PREFIX.length());
        } else {
            supportedHash = passwordHash;
        }
        return BCrypt.checkpw(password, supportedHash);
    }

    static void validateBCryptHash(final String passwordHash) {
        checkBCryptPassword("notused", passwordHash);
    }

    void initialize(String realmName);

    String getName();

    default String maskSubject(String sub) {
        return sub;
    }
}
