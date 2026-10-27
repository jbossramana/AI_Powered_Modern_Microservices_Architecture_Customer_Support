package com.example.nativetools.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
import org.springframework.stereotype.Component;


public class AuditingToolCallingAdvisor extends ToolCallingAdvisor {

    public AuditingToolCallingAdvisor(
            ToolCallingManager manager,
            ToolExecutionEligibilityChecker checker,
            int order,
            boolean conversationHistoryEnabled) {

        super(manager, checker, order, conversationHistoryEnabled);
    }

    @Override
    protected ChatClientRequest doBeforeCall(
            ChatClientRequest request,
            CallAdvisorChain chain) {

        System.out.println("========== BEFORE LLM CALL ==========");
        System.out.println("Prompt: " + request.prompt());

        return super.doBeforeCall(request, chain);
    }

    @Override
    protected ChatClientResponse doAfterCall(
            ChatClientResponse response,
            CallAdvisorChain chain) {

        System.out.println("========== AFTER LLM CALL ==========");

        if (response.chatResponse() != null
                && response.chatResponse().hasToolCalls()) {

            var toolCalls = response.chatResponse()
                    .getResult()
                    .getOutput()
                    .getToolCalls();

            for (var toolCall : toolCalls) {

                System.out.println(
                        "LLM requested tool: " + toolCall.name());

                System.out.println(
                        "Tool arguments: " + toolCall.arguments());

                // Interception point: validate the requested tool.
                if (!isAllowed(toolCall.name())) {
                    throw new SecurityException(
                            "Tool not allowed: " + toolCall.name());
                }

                System.out.println(
                        "Tool approved: " + toolCall.name());
            }
        }

        return super.doAfterCall(response, chain);
    }

    private boolean isAllowed(String toolName) {
        return switch (toolName) {
            case "checkPayment",
                 "checkOrder",
                 "checkInventory" -> true;
            default -> false;
        };
    }
}