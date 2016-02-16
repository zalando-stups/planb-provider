package org.zalando.planb.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice(annotations = { RestController.class })
public class RestControllerAdvice {

    private final Logger log = LoggerFactory.getLogger(RestControllerAdvice.class);

    private final Environment environment;

    @Autowired
    public RestControllerAdvice(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public Object handleExceptions(Exception e) {
        log.error(e.getMessage(), e);
        if (environment.containsProperty("debug") || environment.containsProperty("trace")) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return new ErrorResponse(e.getMessage(), sw.toString());

        }
        return new ErrorResponse(e.getMessage());
    }

}
