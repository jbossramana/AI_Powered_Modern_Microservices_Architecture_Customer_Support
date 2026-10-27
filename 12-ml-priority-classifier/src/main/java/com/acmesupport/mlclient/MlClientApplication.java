package com.acmesupport.mlclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demonstrates calling a traditional ML model from the same architecture used
 * throughout this series - specifically, the exact port-and-adapter shape from
 * clean-architecture-spring-ai. There, TicketClassifierPort had two interchangeable
 * implementations, SpringAiTicketClassifierAdapter and LangChain4jTicketClassifierAdapter
 * - two different GENERATIVE AI frameworks behind one interface. This module adds a
 * third option behind a similarly-shaped port, except MlModelPriorityAdapter isn't an
 * LLM call at all - it's an HTTP call to a trained scikit-learn model. The business
 * code calling the port neither knows nor cares which kind of AI is on the other side.
 */
@SpringBootApplication
public class MlClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MlClientApplication.class, args);
    }
}
