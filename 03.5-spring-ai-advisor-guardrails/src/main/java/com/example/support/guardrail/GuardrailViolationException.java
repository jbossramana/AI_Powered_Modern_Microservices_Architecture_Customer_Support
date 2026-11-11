package com.example.support.guardrail;

public class GuardrailViolationException extends RuntimeException {
    public GuardrailViolationException(String message) {
        super(message);
    }
}
