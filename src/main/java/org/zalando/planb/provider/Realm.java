package org.zalando.planb.provider;

import com.google.common.collect.Lists;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface Realm {

    List<String> NEW_BCRYPT_SALT_PREFIXES = Lists.newArrayList("$2b", "$2y");
    String OLD_BCRYPT_SALT_PREFIX = "$2a";
    String COMMON_BCRYPT_PREFIX = "$2";

    static boolean checkBCryptPassword(final String password, String passwordHash) {
        if (!passwordHash.startsWith(COMMON_BCRYPT_PREFIX)) {
            // looks like the hash was additionally base64 encoded
            passwordHash = new String(Base64.getDecoder().decode(passwordHash), UTF_8);
        }

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


    void initialize(String realmName);

    String getName();
}
