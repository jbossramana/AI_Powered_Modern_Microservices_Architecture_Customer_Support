
package com.acmesupport.chatlc4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for integrating LangChain4j with Ollama.
 *
 * Responsibilities:
 * 1. Create an Ollama chat model bean.
 * 2. Configure the model using values from application.yml.
 * 3. Create a SupportAssistant implementation using LangChain4j AiServices.
 *
 * Spring manages these objects as beans and injects them where required.
 */
@Configuration
public class AssistantConfig {

    /**
     * Creates and configures the chat model that communicates with Ollama.
     *
     * @param baseUrl    Ollama server URL, e.g. http://localhost:11434
     * @param modelName  model name, e.g. llama3.2
     * @param temperature controls response randomness:
     *                    lower values generally produce more predictable answers.
     *
     * @return a ChatModel that can send prompts to the configured Ollama model
     */
    @Bean
    ChatModel ollamaChatModel(
            @Value("${support.ollama.base-url}") String baseUrl,
            @Value("${support.ollama.model-name}") String modelName,
            @Value("${support.ollama.temperature}") Double temperature) {

        // Build the LangChain4j Ollama chat model with the supplied settings.
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }

    /**
     * Creates the application's support assistant.
     *
     * AiServices creates an implementation of SupportAssistant at runtime
     * and connects its methods to the configured chat model.
     *
     * You do not need to write the implementation class yourself.
     *
     * @param ollamaChatModel the ChatModel bean created above
     * @return a ready-to-use SupportAssistant bean
     */
    @Bean
    SupportAssistant supportAssistant(ChatModel ollamaChatModel) {

        // Connect the SupportAssistant interface to the Ollama chat model.
        return AiServices.create(
                SupportAssistant.class,
                ollamaChatModel
        );
    }
}
