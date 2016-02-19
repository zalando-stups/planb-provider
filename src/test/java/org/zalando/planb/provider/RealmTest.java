package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.StrictAssertions.assertThat;

public class RealmTest {
    @Test
    public void testCheckBCryptPassword() {
        final String password = "pass";
        final String pwHash = BCrypt.hashpw(password, BCrypt.gensalt());
        assertThat(Realm.checkBCryptPassword(password, pwHash)).isTrue();
        assertThat(Realm.checkBCryptPassword("wrongpass", pwHash)).isFalse();

        final String newHash = "$2b" + pwHash.substring(3);
        assertThat(Realm.checkBCryptPassword(password, newHash)).isTrue();

        // test with base64-encoded BCrypt hash
        final String encodedHash = Base64.getEncoder().encodeToString(pwHash.getBytes(UTF_8));
        assertThat(Realm.checkBCryptPassword(password, encodedHash)).isTrue();
    }
}
