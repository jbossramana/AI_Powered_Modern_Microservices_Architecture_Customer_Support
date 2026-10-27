package com.acmesupport.springai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class KnowledgeServiceTest {
    private final KnowledgeService knowledge = new KnowledgeService();
    @Test void findsPasswordResetKnowledge() {
        var results = knowledge.search("How do I reset my password?", 3);
        assertFalse(results.isEmpty());
        assertEquals("password-reset", results.getFirst().id());
    }
    @Test void returnsNoResultsForUnrelatedQuestion() {
        assertTrue(knowledge.search("What is the office parking policy?", 3).isEmpty());
    }
}
