package com.example.support.guardrails;

public class GuardrailException extends RuntimeException {
	public GuardrailException(String message) {
		super(message);
	}
}
