package com.example.mcpserver.prompts;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component
public class SupportPrompts {

    @McpPrompt(
            name = "refund-response",
            description = "Create a polite and policy-compliant response to a refund query")
    public GetPromptResult refundResponse(
            @McpArg(name = "customerName", description = "Customer name", required = true)
            String customerName,
            @McpArg(name = "customerIssue", description = "Customer refund issue", required = true)
            String customerIssue) {

        String prompt = """
                You are an enterprise customer-support assistant.

                Write a concise, polite response to this customer.

                Customer name: %s
                Customer issue: %s

                Follow these rules:
                - Do not promise a refund before billing verification.
                - Do not invent transaction IDs, dates, or amounts.
                - Explain the next step clearly.
                - Use a professional and empathetic tone.
                """.formatted(customerName, customerIssue);

        return GetPromptResult.builder(List.of(
                new PromptMessage(
                        Role.USER,
                        TextContent.builder(prompt).build()
                )
        )).description("Refund response prompt").build();
    }

    @McpPrompt(
            name = "ticket-summary",
            description = "Summarize a customer support ticket in a consistent format")
    public GetPromptResult ticketSummary(
            @McpArg(name = "ticketId", description = "Ticket identifier", required = true)
            String ticketId,
            @McpArg(name = "ticketDetails", description = "Ticket details", required = true)
            String ticketDetails) {

        String prompt = """
                You are a senior support analyst.

                Summarize this ticket:
                Ticket ID: %s
                Details: %s

                Return:
                1. Issue summary
                2. Customer impact
                3. Priority recommendation
                4. Suggested next action

                Do not invent missing facts.
                """.formatted(ticketId, ticketDetails);

        return GetPromptResult.builder(List.of(
                new PromptMessage(
                        Role.USER,
                        TextContent.builder(prompt).build()
                )
        )).description("Ticket summary prompt").build();
    }
}
