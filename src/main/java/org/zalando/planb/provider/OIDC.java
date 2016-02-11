package org.zalando.planb.provider;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OIDC {

    @RequestMapping("/")
    String home() {
        return "Hello World!";
    }
}
