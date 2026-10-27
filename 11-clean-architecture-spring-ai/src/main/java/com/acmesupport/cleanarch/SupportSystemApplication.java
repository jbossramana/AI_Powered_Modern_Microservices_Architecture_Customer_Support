package com.acmesupport.cleanarch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bonus module: the triage, RAG-answer, and human-approval use cases from
 * 02-agent-spring-ai, 03-rag-spring-ai, and 06-human-in-the-loop-spring-ai, rebuilt
 * with Clean Architecture (Hexagonal / Ports-and-Adapters) layering instead of each
 * module's flat package. See README.md for the full explanation - short version:
 * source-code dependencies only point inward. domain/ knows nothing about
 * application/; application/ knows nothing about adapter/ or Spring; Spring AI is a
 * detail plugged in at the edges, not something business logic is written against.
 *
 * <p>Package map:
 * <pre>
 * domain/       Entities, value objects, and business rules. Plain Java. No imports
 *               from org.springframework.* or org.springframework.ai.* anywhere.
 * application/  Use cases (application/service) and the ports they depend on
 *               (application/port/in - what the outside world can ask this app to do;
 *               application/port/out - what this app needs FROM the outside world).
 *               Also plain Java - no Spring imports.
 * adapter/      Implementations of those ports. adapter/in/web translates HTTP into
 *               use-case calls; adapter/out/ai and adapter/out/persistence implement
 *               the outbound ports using Spring AI, LangChain4j, and in-memory storage.
 *               This is the ONLY layer allowed to know those technologies exist.
 * config/       Wires it all together: Spring @Bean definitions, application.yml.
 * </pre>
 */
@SpringBootApplication
public class SupportSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportSystemApplication.class, args);
    }
}
