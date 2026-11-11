package com.example.demo.service;

import com.example.demo.model.Customer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;

@Service
public class CustomerPromptService {
    private final ChatClient chatClient;
    private final CustomerService customerService;

    public CustomerPromptService(ChatClient.Builder builder, CustomerService customerService) {
        this.chatClient = builder.build();
        this.customerService = customerService;
    }

    // Output 1: plain text String.
    public String text(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    // Input: separate system and user messages; output: String.
    public String systemAndUser(String question) {
        return chatClient.prompt()
                .system("You are a concise Java and Spring AI trainer.")
                .user(question)
                .call()
                .content();
    }

    // Input: dynamic template values; output: String.
    public String template(String topic, String audience) {
        return chatClient.prompt()
                .system("You are a patient technical instructor.")
                .user(u -> u.text("""
                        Explain the topic {topic} for {audience}.
                        Give a definition, a simple example, and one common mistake.
                        """)
                        .param("topic", topic)
                        .param("audience", audience == null || audience.isBlank()
                                ? "a beginner Java developer" : audience))
                .call()
                .content();
    }

    // Input: Java object fields represented in prompt text; output: String.
    public String summarizeCustomer(Customer customer) {
        return chatClient.prompt()
                .system("Summarize only the supplied customer fields. Do not invent facts.")
                .user("""
                        Customer ID: %d
                        Name: %s
                        City: %s
                        Membership: %s
                        Give a one-sentence summary.
                        """.formatted(customer.id(), customer.name(),
                        customer.city(), customer.membership()))
                .call()
                .content();
    }

    // Input: conversation history; output: String.
    public String withHistory(String previousUser, String previousAssistant,
                              String currentQuestion) {
        List<Message> messages = List.of(
                new SystemMessage("You are a customer-support assistant. Use the prior conversation when relevant."),
                new UserMessage(previousUser),
                new AssistantMessage(previousAssistant),
                new UserMessage(currentQuestion)
        );
        return chatClient.prompt()
                .messages(messages)
                .call()
                .content();
    }

    // Input: retrieved context / RAG-style evidence; output: String.
    public String withDocumentation(String question, String documentation) {
        return chatClient.prompt()
                .system("""
                        Answer using the supplied documentation when it contains the answer.
                        If the documentation does not contain the answer, say so.
                        Do not claim that this demo performed a real database or web search.
                        """)
                .user(u -> u.text("""
                        Documentation:
                        {context}

                        Question:
                        {question}
                        """)
                        .param("context", documentation == null ? "" : documentation)
                        .param("question", question))
                .call()
                .content();
    }

    // Input: tool-eligible customer question; output: String.
    public String askCustomerTool(String question) {
        return chatClient.prompt()
                .system("""
                        You are a customer-support assistant.
                        When customer details by ID are requested, use the customer lookup tool.
                        Base customer facts on the tool result. If no customer is found, say so.
                        """)
                .user(question)
                .tools(customerService)
                .call()
                .content();
    }

    // Output 2: structured response mapped to a Java record.
    public Customer customerEntity(String instruction) {
    	 return chatClient.prompt()
    	            .system("""
    	                You are a data-generation assistant.

    	                Return exactly one customer.
    	                Populate all four fields:
    	                - id: a number
    	                - name: a string
    	                - city: a string
    	                - membership: a string

    	                Use the exact values requested by the user.
    	                Do not return null values.
    	                """)
    	            .user("""
    	                Create a customer using these details:
    	                %s

    	                Return the result as a structured Customer object.
    	                """.formatted(instruction))
    	            .call()
    	            .entity(Customer.class);
    }

    // Output 3: list of structured objects.
    public List<Customer> customerList(String instruction) {
        return chatClient.prompt()
                .system("Return only the requested customer records as structured data.")
                .user(instruction)
                .call()
                .entity(new ParameterizedTypeReference<List<Customer>>() {});
    }

    // Output 4: flexible key-value structure.
    public Map<String, Object> mapOutput(String ticketText) {
        return chatClient.prompt()
                .system("""
                        Classify the ticket. Return a structured object with fields:
                        category, priority, sentiment, recommendedAction.
                        """)
                .user(ticketText)
                .call()
                .entity(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    // Output 5: complete ChatResponse with generation metadata.
    public org.springframework.ai.chat.model.ChatResponse chatResponse(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .chatResponse();
    }

    // Input: image + text; requires a vision-capable Ollama model.
    public String analyzeImage(Resource image, String question) {
        return chatClient.prompt()
                .user(u -> u.text(question)
                        .media(MimeTypeUtils.IMAGE_PNG, image))
                .call()
                .content();
    }
}
