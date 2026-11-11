package com.example.support.guardrails;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
	@ExceptionHandler(GuardrailException.class)
	ResponseEntity<Map<String, String>> guardrail(GuardrailException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException ex) {
		return ResponseEntity.badRequest()
				.body(Map.of("error", "Message is required and must be at most 2000 characters."));
	}
}
