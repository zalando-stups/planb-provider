package org.zalando.planb.provider;

import org.junit.Test;
import java.util.Optional;

public class OIDCControllerTest {

    @Test(expected = RealmNotFoundException.class)
    public void realmNotFound() {
        RealmConfig realms = new RealmConfig(null);
        OIDCController.getRealmName(realms, Optional.empty(), Optional.of("some.header.value"));
    }

}
