package org.zalando.planb.provider;

import org.springframework.http.HttpMethod;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExceptionHandlingITController {

    @RequestMapping(value = "/throwError")
    public void error() {
        throw new RuntimeException("TEST_ERROR");
    }

    @RequestMapping(value = "/methodNotAllowed")
    public void methodNotAllowed() throws HttpRequestMethodNotSupportedException {
        throw new HttpRequestMethodNotSupportedException(HttpMethod.GET.name(), new String[]{"POST"}, "Request method 'GET' not supported");
    }

}
