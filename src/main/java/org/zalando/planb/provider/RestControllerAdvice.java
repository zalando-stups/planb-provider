package org.zalando.planb.provider;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice(annotations = {RestController.class})
public class RestControllerAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(RestControllerAdvice.class);

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Map<String, String>> argumentProcessingExceptions(ServletRequestBindingException e) {
        return status(BAD_REQUEST)
                .body(errorBody("failed_request_binding", e.getMessage()));
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<Map<String, String>> handleRestExceptions(RestException e) {
        LOG.warn("{} (status {} / {})", e.getMessage(), e.getStatusCode(), e.getClass().getSimpleName());
        return status(e.getStatusCode()).body(errorBody(e.getErrorType(), e.getErrorDescription()));
    }

    @ExceptionHandler(HystrixRuntimeException.class)
    public ResponseEntity<Map<String, String>> handleHystrixExceptions(HystrixRuntimeException e) {
        LOG.warn("Dependency unavailable:", e);
        return status(SERVICE_UNAVAILABLE).body(errorBody("unavailable_dependency", "Dependency unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllOtherExceptions(Exception e) {
        LOG.error(e.getMessage() + " (status 500 / " + e.getClass().getSimpleName() + ")", e);
        return status(INTERNAL_SERVER_ERROR).body(errorBody("internal_error", e.getMessage()));
    }

    private static Map<String, String> errorBody(String errorType, String errorMessage) {
        final ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        Optional.ofNullable(errorType).ifPresent(error -> map.put("error", error));
        Optional.ofNullable(errorMessage).ifPresent(message -> map.put("error_description", errorMessage));
        return map.build();
    }
}
