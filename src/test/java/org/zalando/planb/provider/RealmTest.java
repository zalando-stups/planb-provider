package org.zalando.planb.provider;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.assertj.core.api.StrictAssertions.assertThat;

/**
 * Created by hjacobs on 2/15/16.
 */
public class RealmTest {

    @Test
    public void testCheckBCryptPassword() {
        final String password = "pass";
        final String pwHash = BCrypt.hashpw(password, BCrypt.gensalt());
        assertThat(Realm.checkBCryptPassword(password, pwHash)).isTrue();
        assertThat(Realm.checkBCryptPassword("wrongpass", pwHash)).isFalse();

        final String newHash = "$2b" + pwHash.substring(3);
        assertThat(Realm.checkBCryptPassword(password, newHash)).isTrue();
    }
}
