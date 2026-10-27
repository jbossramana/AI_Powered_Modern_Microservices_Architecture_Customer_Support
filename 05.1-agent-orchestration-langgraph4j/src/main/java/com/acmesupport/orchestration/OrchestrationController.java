package com.acmesupport.orchestration;

import com.acmesupport.orchestration.graph.SupportGraphState;
import jakarta.validation.constraints.NotBlank;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OrchestrationController {

    private final CompiledGraph<SupportGraphState> supportGraph;

    public OrchestrationController(CompiledGraph<SupportGraphState> supportGraph) {
        this.supportGraph = supportGraph;
    }

    public record ProcessTicketRequest(
            @NotBlank String ticketId, @NotBlank String customerId, @NotBlank String ticketText) {
    }

    public record TicketOutcome(
            String ticketId, String customerId, String category, String priority, String sentiment,
            String summary, String retrievedContext, String draftResponse, String escalationReason,
            String route) {
    }

    /**
     * POST /api/tickets/process
     * {"ticketId": "TCK-2001", "customerId": "C-1002", "ticketText": "..."}
     *
     * Runs the whole graph synchronously and returns the final state. Try both a calm
     * routine question and an angry, urgent one - watch "route" flip between
     * AUTO_DRAFTED and ESCALATED, and notice draftResponse is empty on the escalated
     * path (that node never ran) while escalationReason is empty on the drafted path.
     */
    @PostMapping("/api/tickets/process")
    public TicketOutcome process(@RequestBody ProcessTicketRequest request) throws Exception {
        Map<String, Object> initialState = Map.of(
                SupportGraphState.TICKET_ID, request.ticketId(),
                SupportGraphState.CUSTOMER_ID, request.customerId(),
                SupportGraphState.TICKET_TEXT, request.ticketText());

        SupportGraphState result = supportGraph.invoke(initialState)
                .orElseThrow(() -> new IllegalStateException("Graph produced no final state"));

        boolean escalated = !result.escalationReason().isBlank();

        return new TicketOutcome(
                result.ticketId(),
                result.customerId(),
                result.category(),
                result.priority(),
                result.sentiment(),
                result.summary(),
                result.retrievedContext(),
                result.draftResponse(),
                result.escalationReason(),
                escalated ? "ESCALATED" : "AUTO_DRAFTED");
    }
}
