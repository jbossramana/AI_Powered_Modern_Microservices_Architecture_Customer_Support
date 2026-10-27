package com.example.support.guardrail;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class OutputGuardrail {

    // Illustrative pattern only; production PII detection needs stronger controls.
    private static final Pattern EMAIL =
            Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b",
                    Pattern.CASE_INSENSITIVE);

    public String validateAndSanitize(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new GuardrailViolationException("The model returned an empty answer.");
        }

        if (EMAIL.matcher(answer).find()) {
            throw new GuardrailViolationException(
                    "The response was blocked because it may contain personal information.");
        }

        return answer;
    }
}
