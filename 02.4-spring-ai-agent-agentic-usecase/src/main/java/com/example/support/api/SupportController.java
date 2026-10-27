package com.example.support.api;

import com.example.support.service.SupportAssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportAssistantService assistant;

    public SupportController(SupportAssistantService assistant) {
        this.assistant = assistant;
    }

    public record AskRequest(@NotBlank String message) {}
    public record InvestigationRequest(
            @NotBlank String goal,
            String customerId,
            String ticketId,
            String orderId,
            String invoiceId) {}

    @PostMapping("/agent")
    public Map<String, Object> agent(@Valid @RequestBody AskRequest request) {
        return Map.of(
                "approach", "LLM-driven agent",
                "description", "The model can select relevant support tools to answer the question.",
                "answer", assistant.answerWithAgent(request.message())
        );
    }

    @PostMapping("/investigate")
    public Map<String, Object> investigate(@Valid @RequestBody InvestigationRequest request) {
        String identifiers = """
                Customer ID: %s
                Ticket ID: %s
                Order ID: %s
                Invoice ID: %s
                """.formatted(value(request.customerId()), value(request.ticketId()),
                              value(request.orderId()), value(request.invoiceId()));
        return Map.of(
                "approach", "Goal-oriented agentic investigation with explicit Java loop",
                "description", "Java controls the maximum tool rounds; the LLM chooses tools based on earlier observations. Tools in this demo are read-only.",
                "goal", request.goal(),
                "answer", assistant.investigateWithExplicitLoop(identifiers, request.goal())
        );
    }

    @GetMapping("/examples")
    public Map<String, Object> examples() {
        return Map.of(
            "agentExamples", new String[] {
                "Where can I find my latest invoice? Invoice ID INV-1001.",
                "What is the status of ticket TCK-1001?",
                "Is the cloud service available right now?",
                "How much storage is included in customer C-1001's plan?"
            },
            "agenticExamples", new String[] {
                "Investigate why customer C-1001 was charged more than the invoice amount. Compare the invoice and transactions.",
                "Investigate why ticket TCK-1001 has been open for five days. Check status, SLA and assignment history.",
                "Investigate why uploads are failing for customer C-1001 even though storage usage is within the plan limit.",
                "Investigate why the application became slow after deployment DEP-2001. Check health, deployment history and performance evidence."
            }
        );
    }

    private static String value(String input) {
        return input == null || input.isBlank() ? "not provided" : input;
    }
}
