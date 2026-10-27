package com.acmesupport.chatlc4j;

import dev.langchain4j.service.SystemMessage;

/**
 * A LangChain4j "AI Service": you declare the shape of the conversation as a plain Java
 * interface, and {@link dev.langchain4j.service.AiServices} generates the implementation
 * (prompt building, calling the model, parsing the response) at runtime - conceptually
 * similar to a Spring Data repository interface, but for LLM calls.
 */
public interface SupportAssistant {

    @SystemMessage("""
            You are Aria, a customer support assistant for Acme Cloud Storage.
            Be concise, friendly, and factual. If you don't know something, say so
            instead of guessing. Do not make up order numbers, refund amounts, or
            policy details that were not given to you.
            """)
    String chat(String userMessage);
}
