package com.example.mcpclient.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mcpclient.service.McpAccessService;

@RestController
public class McpDemoController {

    private final McpAccessService mcpAccessService;

    public McpDemoController(McpAccessService mcpAccessService) {
        this.mcpAccessService = mcpAccessService;
    }

    @GetMapping("/api/mcp/catalog")
    public Map<String, Object> catalog() {
        return mcpAccessService.catalog();
    }

    @GetMapping("/api/mcp/resource")
    public Object readResource(
            @RequestParam(defaultValue = "support://policies/refund") String uri) {
        return mcpAccessService.readResource(uri);
    }

    @GetMapping("/api/mcp/prompt")
    public Object getPrompt(
            @RequestParam(defaultValue = "refund-response") String name,
            @RequestParam(defaultValue = "Ravi") String customerName,
            @RequestParam(defaultValue = "I was charged twice") String customerIssue,
            @RequestParam(defaultValue = "T1001") String ticketId,
            @RequestParam(defaultValue = "Duplicate subscription charge; ticket is OPEN and priority HIGH") String ticketDetails) {

        if ("ticket-summary".equals(name)) {
            return mcpAccessService.getPrompt(
                    name,
                    Map.of(
                            "ticketId", ticketId,
                            "ticketDetails", ticketDetails
                    )
            );
        }

        return mcpAccessService.getPrompt(
                name,
                Map.of(
                        "customerName", customerName,
                        "customerIssue", customerIssue
                )
        );
    }
}
