package com.acmesupport.chatlc4j;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {
	private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>();

	public KnowledgeService() {
		add("password-reset", "Password reset",
				"To reset a password, open the sign-in page, choose Forgot password, and follow the email verification link. Links expire after 30 minutes. Contact support if the email does not arrive.");
		add("outage", "Production outage",
				"For a production outage, create a high-priority support ticket and include affected region, start time, error message, and business impact. Do not include passwords or access tokens.");
		add("billing", "Billing questions",
				"Billing adjustments require account verification and review by the billing team. The assistant must not promise a refund or invent an amount.");
		add("security", "Security incident",
				"For suspected account compromise, revoke active sessions, rotate credentials using the approved identity portal, and contact the security response team immediately.");
	}

	public synchronized void add(String id, String title, String text) {
		chunks.add(new KnowledgeChunk(id, title, text));
	}

	public List<KnowledgeChunk> search(String query, int limit) {
		Set<String> terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
				.filter(s -> s.length() > 2).collect(Collectors.toSet());
		return chunks.stream().map(c -> Map.entry(c, score(c, terms))).filter(e -> e.getValue() > 0)
				.sorted(Map.Entry.<KnowledgeChunk, Integer>comparingByValue().reversed()).limit(limit)
				.map(Map.Entry::getKey).toList();
	}

	private int score(KnowledgeChunk c, Set<String> terms) {
		String hay = (c.title() + " " + c.text()).toLowerCase(Locale.ROOT);
		int n = 0;
		for (String term : terms)
			if (hay.contains(term))
				n++;
		return n;
	}

	public List<KnowledgeChunk> all() {
		return List.copyOf(chunks);
	}

	public record KnowledgeChunk(String id, String title, String text) {
	}
}
