package com.acmesupport.cleanarch.adapter.in.web;

import com.acmesupport.cleanarch.application.port.in.TriageTicketUseCase;
import com.acmesupport.cleanarch.application.port.in.TriageTicketUseCase.TriageCommand;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * An inbound adapter: translates HTTP into a use-case call and a use-case result back
 * into HTTP. This class is allowed to know Spring MVC exists - that's its whole job -
 * but it depends on TriageTicketUseCase (an interface owned by application/), never on
 * TriageTicketService (the implementation) directly.
 */
@RestController
public class TicketController {

    private final TriageTicketUseCase triageTicketUseCase;

    public TicketController(TriageTicketUseCase triageTicketUseCase) {
        this.triageTicketUseCase = triageTicketUseCase;
    }

    public record TriageHttpRequest(@NotBlank String customerId, @NotBlank String ticketText) {
    }

    /**
     * A dedicated web response DTO, explicitly mapped from the use case's result
     * rather than serializing a domain type straight to JSON. This is the stricter of
     * two reasonable approaches shown in this codebase - compare ApprovalController,
     * which takes the more pragmatic route of returning a domain type directly. Either
     * is defensible; see the README for the tradeoff.
     */
    public record TriageHttpResponse(
            String category, String priority, String sentiment, String summary, boolean requiresHumanEscalation) {

        static TriageHttpResponse from(TriageTicketUseCase.TriageResult result) {
            var classification = result.classification();
            return new TriageHttpResponse(
                    classification.category().name(),
                    classification.priority().name(),
                    classification.sentiment().name(),
                    classification.summary(),
                    result.requiresHumanEscalation());
        }
    }

    /**
     * POST /api/tickets/triage
     * {"customerId": "C-1002", "ticketText": "..."}
     */
    @PostMapping("/api/tickets/triage")
    public TriageHttpResponse triage(@RequestBody TriageHttpRequest request) {
        var result = triageTicketUseCase.triage(new TriageCommand(request.customerId(), request.ticketText()));
        return TriageHttpResponse.from(result);
    }
}
