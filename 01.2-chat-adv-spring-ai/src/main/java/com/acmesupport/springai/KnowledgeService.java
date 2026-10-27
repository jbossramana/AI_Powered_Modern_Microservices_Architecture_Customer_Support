package com.acmesupport.springai;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
@Service
public class KnowledgeService {
    public record KnowledgeChunk(String id, String title, String text) {}
    private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>(List.of(
        new KnowledgeChunk("password-reset", "Password reset", "To reset a password, open the sign-in page, select Forgot password, and follow the verified email instructions. Never share passwords or one-time codes with support."),
        new KnowledgeChunk("account-compromise", "Suspected account compromise", "If an account may be compromised, contact the security team immediately, reset the password from a trusted device, revoke active sessions if available, and enable multi-factor authentication."),
        new KnowledgeChunk("refund-window", "Refund policy", "Eligible subscription refunds must be requested within 14 days of the original purchase. Billing support verifies eligibility before a refund is approved.")
    ));
    public List<KnowledgeChunk> search(String question, int limit) {
        Set<String> terms = terms(question);
        return chunks.stream().map(c -> Map.entry(c, score(c, terms)))
            .filter(e -> e.getValue() > 0)
            .sorted(Map.Entry.<KnowledgeChunk, Integer>comparingByValue().reversed())
            .limit(limit).map(Map.Entry::getKey).toList();
    }
    private int score(KnowledgeChunk c, Set<String> terms) {
        Set<String> doc = terms(c.title() + " " + c.text());
        int score = 0;
        for (String term : terms) if (doc.contains(term)) score++;
        return score;
    }
    private Set<String> terms(String text) {
        Set<String> stop = Set.of("what","how","when","where","does","should","could","would","the","and","for","from","with","that","this","are","is","may","can","you","your","if","a","an","to","of","in","on");
        Set<String> result = new HashSet<>();
        for (String t : text.toLowerCase(Locale.ROOT).split("[^a-z0-9-]+"))
            if (t.length() > 2 && !stop.contains(t)) result.add(t);
        return result;
    }
    public List<KnowledgeChunk> all() { return List.copyOf(chunks); }
    public synchronized KnowledgeChunk add(String id, String title, String text) {
        if (chunks.stream().anyMatch(c -> c.id().equals(id))) throw new IllegalArgumentException("Knowledge id already exists: " + id);
        KnowledgeChunk chunk = new KnowledgeChunk(id, title, text);
        chunks.add(chunk);
        return chunk;
    }
}
