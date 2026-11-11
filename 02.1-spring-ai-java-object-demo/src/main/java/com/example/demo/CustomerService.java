package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public CustomerService(ChatClient.Builder chatClientBuilder,
                           ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public CustomerSummary summarizeCustomer(Customer customer)
            throws JsonProcessingException {

        String customerJson = objectMapper.writeValueAsString(customer);

        return chatClient.prompt()
                .system("""
                        You are a customer analysis assistant.
                        Analyze the supplied customer data.
                        Return a brief summary and one useful recommendation.
                        """)
                .user(u -> u.text("""
                        Analyze this customer data:

                        {customerData}
                        """)
                        .param("customerData", customerJson))
                .call()
                .entity(CustomerSummary.class);
    }
}
