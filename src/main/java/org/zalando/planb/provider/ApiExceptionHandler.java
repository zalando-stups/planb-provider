package org.zalando.planb.provider;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.status;

// TODO maybe merge this later into jbellmanns error handler
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RealmNotManagedException.class)
    public ResponseEntity<Map<String, String>> handleRealmNotManaged(RealmNotManagedException e) {
        return status(BAD_REQUEST).body(singletonMap("error_message", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException e) {
        return status(NOT_FOUND).body(singletonMap("error_message", e.getMessage()));
    }
}
