package com.example.sequentialtools.web;

import com.example.sequentialtools.model.InvestigationModels;
import com.example.sequentialtools.service.SequentialAgentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestigationController {
	private final SequentialAgentService agent;

	public InvestigationController(SequentialAgentService agent) {
		this.agent = agent;
	}

	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP", "application", "sequential-three-tools");
	}

	@PostMapping("/investigate")
	public InvestigationModels.InvestigationResponse investigate(
			@RequestBody InvestigationModels.InvestigateRequest req) {
		if (req.orderId() == null || req.orderId().isBlank())
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
		String question = req.question() == null || req.question().isBlank()
				? "Check payment, order status and inventory."
				: req.question().trim();
		int max = req.maxToolCalls() == null ? 3 : req.maxToolCalls();
		if (max < 1 || max > 3)
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxToolCalls must be between 1 and 3");
		return agent.investigate(req.orderId().trim(), question, max);
	}
}
