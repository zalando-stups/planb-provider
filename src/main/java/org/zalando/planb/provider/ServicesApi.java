package org.zalando.planb.provider;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = "/services")
public class ServicesApi {

    @RequestMapping(method = GET)
    public List<ServiceSummary> listServices() {
        return asList(new ServiceSummary("foo"), new ServiceSummary("bar"));
    }
}
