package com.example.support.knowledge;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/** Small, dependency-free demonstration retriever. Replace with an embedding-backed VectorStore for production. */
@Component
public class KnowledgeBase {
    public record Article(String id, String title, String text) {}
    private final List<Article> articles = List.of(
        new Article("KB-101", "Refund policy", "Refund eligibility depends on the product and payment status. A duplicate payment should be reviewed by billing support. This assistant can explain the policy but cannot issue refunds."),
        new Article("KB-102", "Ticket status", "Customers can ask support for ticket status using the ticket ID. Example ticket TCK-1001 is currently OPEN and assigned to the Application Support team. Expected first response: within 4 business hours."),
        new Article("KB-103", "Login troubleshooting", "If login fails, verify the registered email, reset the password, and check the service status page. Never share passwords or one-time passcodes with support."),
        new Article("KB-104", "Escalation policy", "Production outages affecting multiple regions are high priority. Provide the incident ID, affected regions, start time, and business impact. The on-call team should be paged by an authorized operator."),
        new Article("KB-105", "Privacy policy", "Do not request or expose full payment card numbers, passwords, authentication codes, or unnecessary personal data. Verify customer identity through approved authenticated channels.")
    );

    public List<Article> search(String question, int limit) {
        Set<String> terms = Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+"))
                .filter(t -> t.length() > 2).collect(Collectors.toSet());
        return articles.stream().map(a -> Map.entry(a, score(a, terms)))
                .filter(e -> e.getValue() > 0).sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit).map(Map.Entry::getKey).toList();
    }
    private int score(Article a, Set<String> terms) {
        String corpus = (a.title() + " " + a.text()).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String term : terms) if (corpus.contains(term)) score++;
        return score;
    }
}
