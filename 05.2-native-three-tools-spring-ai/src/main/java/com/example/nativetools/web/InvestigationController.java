package com.example.nativetools.web;

import com.example.nativetools.model.InvestigationRequest;
import com.example.nativetools.service.OrderInvestigationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestigationController {
    private final OrderInvestigationService service;
    public InvestigationController(OrderInvestigationService service) { this.service = service; }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "application", "native-tool-calling");
    }

    @PostMapping("/investigate")
    public Map<String, String> investigate(@RequestBody InvestigationRequest request) {
        if (request.orderId() == null || request.orderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        String question = request.question() == null || request.question().isBlank()
            ? "Check payment, order shipment, and inventory for this order."
            : request.question().trim();
        String answer = service.investigate(request.orderId().trim(), question);
        return Map.of("orderId", request.orderId().trim(), "answer", answer);
    }
}
