package com.example.support.guardrails;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class OutputGuardrail {
	private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
	private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?\\d[ .()-]?){9,13}(?!\\d)");

	public String sanitize(String answer) {
		if (answer == null || answer.isBlank())
			return "I couldn't produce a reliable answer. Please contact a support representative.";
		String safe = EMAIL.matcher(answer).replaceAll("[REDACTED_EMAIL]");
		safe = PHONE.matcher(safe).replaceAll("[REDACTED_PHONE]");
		// Avoid allowing the model to promise a financial action it cannot execute.
		safe = safe.replaceAll(
				"(?i)\\b(your refund has been issued|we have refunded your payment|your account has been deleted)\\b",
				"I cannot confirm that action here; a support representative must verify it");
		if (safe.length() > 4000)
			safe = safe.substring(0, 4000) + "…";
		return safe;
	}
}
