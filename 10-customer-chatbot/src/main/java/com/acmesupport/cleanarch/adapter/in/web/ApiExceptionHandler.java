package com.acmesupport.cleanarch.adapter.in.web;

import com.acmesupport.cleanarch.domain.exception.CustomerNotFoundException;
import com.acmesupport.cleanarch.domain.exception.DraftNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translating domain/application exceptions into HTTP status codes is a web-adapter
 * concern. CustomerNotFoundException, DraftNotFoundException, and the IllegalStateException
 * thrown from DraftResponseService.send() are all thrown from deep inside the
 * application layer with no idea what HTTP is - this class is where that gets decided,
 * in one place, instead of scattered across every controller.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFound(CustomerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DraftNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDraftNotFound(DraftNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
