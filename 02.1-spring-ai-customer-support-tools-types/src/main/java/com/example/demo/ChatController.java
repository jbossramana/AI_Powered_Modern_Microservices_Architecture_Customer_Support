package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {
	private final ChatClient chatClient;
	private final CustomerTools customerTools;

	public ChatController(ChatClient.Builder builder, CustomerTools customerTools) {
		this.chatClient = builder.build();
		this.customerTools = customerTools;
	}

	@PostMapping("/customer")
	public String chat(@RequestBody CustomerRequest request) {
		String userPrompt = String.format("""
				Customer ID: %d
				Original price: %.2f
				Discount percentage: %d
				Check eligibility: %s
				Requested products: %s

				Retrieve the customer using the customer ID.
				Calculate the discounted price using the original price and discount percentage.
				If check eligibility is true, check eligibility using the discounted price.
				Retrieve the product list if it helps answer the request.
				Summarize the results clearly. Do not invent tool results.
				""", request.customerId(), request.price(), request.discountPercentage(), request.checkEligibility(),
				request.requestedProducts());

		return chatClient.prompt().advisors(new SimpleLoggerAdvisor()).system("""
				You are a concise customer support assistant.
				Use the provided tools to retrieve or calculate information.
				Base your answer on actual tool results.
				""").user(userPrompt).tools(customerTools).call().content();
	}
}
