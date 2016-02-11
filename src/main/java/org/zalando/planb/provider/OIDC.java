package org.zalando.planb.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
public class OIDC {

    @Autowired
    private Realm realm;

    @RequestMapping(value = "/oauth2/access_token", method = RequestMethod.POST)
    @ResponseBody
    OIDCCreateTokenResponse createToken(@RequestParam(value = "grant_type", required = true) String grantType,
                                        @RequestParam(value = "username", required = true) String username,
                                        @RequestParam(value = "password", required = true) String password,
                                        @RequestParam(value = "scope", required = false) String scope) {
        return new OIDCCreateTokenResponse();
    }

    @RequestMapping("/.well-known/openid-configuration")
    @ResponseBody
    OIDCDiscoveryInformationResponse getDiscoveryInformation() {
        return new OIDCDiscoveryInformationResponse();
    }

    @RequestMapping("/oauth2/v3/certs")
    @ResponseBody
    OIDCSigningKeysResponse getSigningKeys() {
        return new OIDCSigningKeysResponse();
    }
}
