
package com.example.nativetools.service;

import com.example.nativetools.advisor.AuditingToolCallingAdvisor;
import com.example.nativetools.tools.OrderInvestigationTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderInvestigationService {

    private final ChatClient chatClient;
    private final OrderInvestigationTools tools;
    private final AuditingToolCallingAdvisor auditingAdvisor;

    public OrderInvestigationService(
            ChatClient.Builder builder,
            OrderInvestigationTools tools,
            AuditingToolCallingAdvisor auditingAdvisor) {

        this.chatClient = builder.build();
        this.tools = tools;
        this.auditingAdvisor = auditingAdvisor;
    }

    public String investigate(String orderId, String question) {

        String runId = UUID.randomUUID()
                .toString()
                .substring(0, 8);

        System.out.println(
                "\n==============================================================");
        System.out.println(
                "NATIVE SPRING AI TOOL-CALLING RUN: " + runId);
        System.out.println("Order ID: " + orderId);
        System.out.println("Question: " + question);

        System.out.println(
                "[APP -> LLM] Sending question and native tool definitions.");
        System.out.println(
                "[INFO] Custom AuditingToolCallingAdvisor registered.");

        System.out.println(
                "==============================================================");

        String answer = chatClient.prompt()
                .system("""
                        You are a customer-support investigation agent
                        with three read-only tools:
                        checkPayment, checkOrder, checkInventory.

                        IMPORTANT:
                        - Request only ONE tool call at a time.
                        - Never request multiple tools in parallel.
                        - After each tool result, review it before deciding
                          whether to call the next tool.
                        - Usually check payment first, then order/shipment,
                          then inventory.
                        - You may stop early only if the question is answered.
                        - Do not invent tool results or claim you changed
                          an order, shipped an item, or issued a refund.
                        - After the necessary checks, provide a concise
                          customer-facing summary and safe next step.
                        """)
                .user("Order ID: " + orderId
                        + "\nCustomer question: " + question)
                .tools(tools)
                .advisors(auditingAdvisor)
                .call()
                .content();

        System.out.println(
                "\n---------------------- FINAL LLM ANSWER ----------------------");
        System.out.println(answer);
        System.out.println(
                "NATIVE TOOL-CALLING RUN FINISHED: " + runId);
        System.out.println(
                "==============================================================\n");

        return answer;
    }
}
