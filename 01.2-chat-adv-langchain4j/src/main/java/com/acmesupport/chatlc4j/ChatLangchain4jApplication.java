package com.acmesupport.chatlc4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Step 0b: the same "just chat" use case as {@code 00-chat-spring-ai}, built
 * with LangChain4j instead of Spring AI. Spring Boot here is only providing the
 * web layer and dependency injection - the model wiring in
 * {@link AssistantConfig} is plain LangChain4j.
 *
 * <p>
 * Compare this module side-by-side with {@code 00-chat-spring-ai} to see the
 * two frameworks' philosophies: Spring AI leans on Spring Boot
 * autoconfiguration and a fluent {@code ChatClient}; LangChain4j leans on a
 * declarative, interface-based "AI Service" ({@link SupportAssistant}) that you
 * implement with annotations, not code.
 */
@SpringBootApplication
public class ChatLangchain4jApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatLangchain4jApplication.class, args);
	}
}
