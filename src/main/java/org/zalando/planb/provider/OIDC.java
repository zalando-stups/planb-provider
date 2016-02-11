package org.zalando.planb.provider;

import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
public class OIDC {

    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "Hello World!";
    }
}
