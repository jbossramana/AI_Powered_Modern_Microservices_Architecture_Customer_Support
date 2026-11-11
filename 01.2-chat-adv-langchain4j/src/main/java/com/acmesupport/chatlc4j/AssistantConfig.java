package com.acmesupport.chatlc4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class AssistantConfig {
	@Bean
	@ConditionalOnProperty(name = "support.provider", havingValue = "ollama", matchIfMissing = true)
	ChatModel ollamaChatModel(@Value("${support.ollama.base-url}") String baseUrl,
			@Value("${support.ollama.model-name}") String modelName,
			@Value("${support.ollama.temperature:0.2}") Double temperature) {
		return OllamaChatModel.builder().baseUrl(baseUrl).modelName(modelName).temperature(temperature)
				.timeout(Duration.ofSeconds(90)).build();
	}

	@Bean
	@ConditionalOnProperty(name = "support.provider", havingValue = "openai")
	ChatModel openAiChatModel(@Value("${OPENAI_API_KEY:}") String apiKey,
			@Value("${support.openai.model-name:gpt-4o-mini}") String modelName) {
		if (apiKey == null || apiKey.isBlank())
			throw new IllegalStateException("Set OPENAI_API_KEY when support.provider=openai");
		return dev.langchain4j.model.openai.OpenAiChatModel.builder().apiKey(apiKey).modelName(modelName).build();
	}

	@Bean
	SupportAssistant supportAssistant(ChatModel model, SupportTools tools) {
		return AiServices.builder(SupportAssistant.class).chatModel(model).tools(tools).build();
	}

	@Bean
	MemoryAssistant memoryAssistant(ChatModel model) {
		return AiServices.builder(MemoryAssistant.class).chatModel(model)
				.chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20)).build();
	}

	@Bean
	TriageAssistant triageAssistant(ChatModel model) {
		return AiServices.builder(TriageAssistant.class).chatModel(model).build();
	}
}
