package com.example.customer;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class CustomerAiController {

    private final ChatClient chatClient;
    private final CustomerToolService customerService;

    public CustomerAiController(
            ChatClient.Builder chatClientBuilder,
            CustomerToolService customerService) {

        this.chatClient = chatClientBuilder.build();
        this.customerService = customerService;
    }

    // 1. Natural-language response
    @PostMapping("/ask")
    public String ask(@RequestBody AiRequest request) {

        return chatClient.prompt()
                .system("""
                    You are a customer support assistant.
                    Use the customer lookup tool when the user
                    asks for details about a customer by ID.
                    Base your answer on the tool result.
                    Do not invent customer data.
                    If the customer is not found, explain clearly.
                    """)
                .user(request.question())
                .tools(customerService)
                .call()
                .content();
    }

    // 2. JSON-formatted response as a String
    @PostMapping("/ask/json")
    public String askJson(@RequestBody AiRequest request) {

        return chatClient.prompt()
                .system("""
                    You are a customer support assistant.
                    Use the customer lookup tool when needed.
                    Base the response on the tool result.

                    Return the final answer as valid JSON only.
                    Use exactly these fields:
                    id, name, city, membership.
                    Do not include Markdown code fences.
                    Do not invent customer data.
                    """)
                .user(request.question())
                .tools(customerService)
                .call()
                .content();
    }

    // 3. Structured response converted to Customer
    @PostMapping("/ask/customer")
    public Customer askCustomer(
            @RequestBody AiRequest request) {

        return chatClient.prompt()
                .system("""
                    You are a customer support assistant.
                    Use the customer lookup tool when the user
                    asks for customer details.
                    Base the response on the tool result.
                    Do not invent customer data.
                    Return the customer details in the requested
                    structured format.
                    """)
                .user(request.question())
                .tools(customerService)
                .call()
                .entity(Customer.class);
    }

    public record AiRequest(String question) {
    }
}
