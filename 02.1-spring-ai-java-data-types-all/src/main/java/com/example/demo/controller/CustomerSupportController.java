package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.CustomerPromptService;
import com.example.demo.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CustomerSupportController {
    private final CustomerPromptService promptService;
    private final CustomerService customerService;

    public CustomerSupportController(CustomerPromptService promptService,
                                     CustomerService customerService) {
        this.promptService = promptService;
        this.customerService = customerService;
    }

    // Direct REST/database-style lookup: no LLM.
    @GetMapping("/customers/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        Customer customer = customerService.findById(id);
        if (customer == null) {
            throw new CustomerNotFoundException(id);
        }
        return customer;
    }

    // Input: plain String in request DTO. Output: String.
    @PostMapping("/chat")
    public String chat(@Valid @RequestBody ChatRequest request) {
        return promptService.text(request.message());
    }

    // Input: system + user messages. Output: String.
    @PostMapping("/chat/system")
    public String systemChat(@Valid @RequestBody ChatRequest request) {
        return promptService.systemAndUser(request.message());
    }

    // Input: template variables. Output: String.
    @PostMapping("/chat/template")
    public String template(@Valid @RequestBody TemplateRequest request) {
        return promptService.template(request.topic(), request.audience());
    }

    // Input: Java object fields. Output: String.
    @PostMapping("/chat/customer-summary")
    public String customerSummary(@RequestBody Customer customer) {
        return promptService.summarizeCustomer(customer);
    }

    // Input: chat history. Output: String.
    @PostMapping("/chat/history")
    public String history(@Valid @RequestBody HistoryRequest request) {
        return promptService.withHistory(request.previousUserMessage(),
                request.previousAssistantMessage(), request.currentQuestion());
    }

    // Input: retrieved documentation/context. Output: String.
    @PostMapping("/chat/context")
    public String context(@Valid @RequestBody AnalysisRequest request) {
        return promptService.withDocumentation(request.ticketText(), request.documentation());
    }

    // Input: tool-capable customer question. Output: String.
    @PostMapping("/chat/tool")
    public String tool(@Valid @RequestBody ChatRequest request) {
        return promptService.askCustomerTool(request.message());
    }

    // Output: structured Java Customer record (model-generated; validate in real applications).
    @PostMapping("/output/customer")
    public Customer customerOutput(@Valid @RequestBody ChatRequest request) {
        return promptService.customerEntity(request.message());
    }

    // Output: List<Customer>.
    @PostMapping("/output/customers")
    public List<Customer> customersOutput(@Valid @RequestBody ChatRequest request) {
        return promptService.customerList(request.message());
    }

    // Output: Map<String,Object>.
    @PostMapping("/output/map")
    public Map<String, Object> mapOutput(@Valid @RequestBody ChatRequest request) {
        return promptService.mapOutput(request.message());
    }

    // Output: ChatResponse; JSON serialization exposes response fields available in the version/provider.
    @PostMapping("/output/chat-response")
    public ChatResponse chatResponse(@Valid @RequestBody ChatRequest request) {
        return promptService.chatResponse(request.message());
    }

    // Input: uploaded PNG + text; configure a vision model such as llava for this endpoint.
    @PostMapping(value = "/chat/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String image(@RequestPart("image") MultipartFile image,
                        @RequestPart("question") String question) throws IOException {
        Resource resource = image.getResource();
        return promptService.analyzeImage(resource, question);
    }

    public record ErrorResponse(String error) {}

    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(CustomerNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }
}
