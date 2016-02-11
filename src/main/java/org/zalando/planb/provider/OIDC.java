package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OIDC {

    private final Realm realm;

    @Autowired
    public OIDC(Realm realm) {
        this.realm = realm;
    }

    @RequestMapping(value = "/oauth2/access_token", method = RequestMethod.POST)
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,
                                        @RequestParam(value = "scope", required = false) String scope) {
        return new OIDCCreateTokenResponse();
    }

    @RequestMapping("/.well-known/openid-configuration")
    OIDCDiscoveryInformationResponse getDiscoveryInformation() {
        return new OIDCDiscoveryInformationResponse();
    }

    @RequestMapping("/oauth2/v3/certs")
    OIDCSigningKeysResponse getSigningKeys() {
        return new OIDCSigningKeysResponse();
    }
}
