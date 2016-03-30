package org.zalando.planb.provider;

import com.google.common.collect.ImmutableMap;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.status;

@ControllerAdvice
@Slf4j
public class RestControllerAdvice {

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Map<String, String>> argumentProcessingExceptions(ServletRequestBindingException e) {
        return status(HttpStatus.BAD_REQUEST)
                .body(errorBody("failed_request_binding", e.getMessage()));
    }

    @ExceptionHandler(RestException.class)
    public ResponseEntity<Map<String, String>> handleRestExceptions(RestException e) {
        log.warn("{} (status {} / {})", e.getMessage(), e.getStatusCode(), e.getClass().getSimpleName());
        return status(e.getStatusCode()).body(errorBody(e.getErrorType(), e.getErrorDescription()));
    }

    @ExceptionHandler(HystrixRuntimeException.class)
    public ResponseEntity<Map<String, String>> handleHystrixExceptions(HystrixRuntimeException e) {
        log.warn("Dependency unavailable:", e);
        return status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody("unavailable_dependency", "Dependency unavailable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllOtherExceptions(Exception e) {
        log.error(e.getMessage() + " (status 500 / " + e.getClass().getSimpleName() + ")", e);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody("internal_error", e.getMessage()));
    }

    private static Map<String, String> errorBody(String errorType, String errorMessage) {
        final ImmutableMap.Builder<String, String> map = ImmutableMap.builder();
        Optional.ofNullable(errorType).ifPresent(error -> map.put("error", error));
        Optional.ofNullable(errorMessage).ifPresent(message -> map.put("error_description", errorMessage));
        return map.build();
    }
}
