package com.acmesupport.chatlc4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KnowledgeServiceTest {
    @Test void findsRelevantPasswordArticle() {
        KnowledgeService service = new KnowledgeService();
        var results = service.search("password reset email", 3);
        assertFalse(results.isEmpty());
        assertEquals("password-reset", results.get(0).id());
    }

    @Test void irrelevantQueryReturnsNoMatches() {
        KnowledgeService service = new KnowledgeService();
        assertTrue(service.search("quantum spaceship recipe", 3).isEmpty());
    }

    @Test void canAddKnowledgeItem() {
        KnowledgeService service = new KnowledgeService();
        service.add("mfa-test", "MFA", "Use an authenticator app.");
        assertEquals("mfa-test", service.search("authenticator", 3).get(0).id());
    }
}
