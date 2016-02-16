package org.zalando.planb.provider;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionHandlingITController {

    @RequestMapping(value = "/throwError")
    public void error() {
        throw new RuntimeException("TEST_ERROR");
    }
}
