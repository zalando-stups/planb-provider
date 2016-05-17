package org.zalando.planb.provider;

import org.junit.Test;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class OIDCControllerTest {

    @Test(expected = RealmNotFoundException.class)
    public void realmNotFound() {
        RealmConfig realms = new RealmConfig(null);
        OIDCController.getRealmName(realms, Optional.empty(), Optional.of("some.header.value"));
    }

    @Test(expected = BadRequestException.class)
    public void invalidAuthorizationThrowsBadRequest1() {
        OIDCController.getClientCredentials(Optional.of("Basic NO-SET-AUTHORIZATION"));
    }

    @Test(expected = BadRequestException.class)
    public void invalidAuthorizationThrowsBadRequest2() {
        OIDCController.getClientCredentials(Optional.of("invalid_base64"));
    }

    @Test(expected = BadRequestException.class)
    public void invalidAuthorizationThrowsBadRequest3() {
        OIDCController.getClientCredentials(Optional.of(""));
    }

    @Test(expected = BadRequestException.class)
    public void invalidAuthorizationThrowsBadRequest4() {
        OIDCController.getClientCredentials(Optional.ofNullable(null));
    }

    @Test
    public void resilientToIncompleteAuthorizationHeader() {
        ClientCredentials cred1 = OIDCController.getClientCredentials(Optional.of("Basic dXNlcm5hbWU6"));
        assertEquals(cred1.getClientId(), "username");
        assertEquals(cred1.getClientSecret(), "");
        ClientCredentials cred2 = OIDCController.getClientCredentials(Optional.of("Basic Og=="));
        assertEquals(cred2.getClientId(), "");
        assertEquals(cred2.getClientSecret(), "");
    }
}
