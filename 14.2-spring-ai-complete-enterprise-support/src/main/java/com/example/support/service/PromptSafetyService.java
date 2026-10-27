package com.example.support.service;

import org.springframework.stereotype.Service;

@Service
public class PromptSafetyService {

    public String validateCustomerText(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Customer message must not be blank.");
        }

        if (input.length() > 8_000) {
            throw new IllegalArgumentException("Customer message exceeds the 8000-character limit.");
        }

        // Lightweight example only. This is not a complete prompt-injection detector.
        String normalized = input.toLowerCase();
        if (normalized.contains("ignore all previous instructions")
                || normalized.contains("reveal the system prompt")
                || normalized.contains("show hidden instructions")) {
            throw new IllegalArgumentException(
                    "The message contains a disallowed instruction pattern."
            );
        }

        return input.strip();
    }
}
