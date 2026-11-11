package com.example.support.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurityException(
            SecurityException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("Tool call rejected by security policy: "
                        + ex.getMessage());
    }
}