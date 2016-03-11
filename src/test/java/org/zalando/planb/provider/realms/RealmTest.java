package org.zalando.planb.provider.realms;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.zalando.planb.provider.realms.Realm;

import static org.assertj.core.api.Assertions.assertThat;

public class RealmTest {
    @Test
    public void testCheckBCryptPassword() {
        final String password = "pass";
        final String pwHash = BCrypt.hashpw(password, BCrypt.gensalt(4));
        assertThat(Realm.checkBCryptPassword(password, pwHash)).isTrue();
        assertThat(Realm.checkBCryptPassword("wrongpass", pwHash)).isFalse();

        final String newHash = "$2b" + pwHash.substring(3);
        assertThat(Realm.checkBCryptPassword(password, newHash)).isTrue();
    }
}
