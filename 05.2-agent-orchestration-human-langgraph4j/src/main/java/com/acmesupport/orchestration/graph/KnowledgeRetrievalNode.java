package com.acmesupport.orchestration.graph;

import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Deliberately simplified retrieval: keyword overlap against four hardcoded articles,
 * not embeddings. The point of this node is to show <em>where</em> a retrieval step
 * fits in a multi-node graph, not to re-teach vector search - see Module 3 for the real
 * thing. Swap the body of {@link #apply} for a call through a
 * {@code VectorStoreDocumentRetriever} (Module 3's approach) and nothing else in this
 * graph - not the state shape, not the edges - needs to change.
 */
@Component
public class KnowledgeRetrievalNode implements NodeAction<SupportGraphState> {

    private record Article(String title, String content) {
    }

    private final List<Article> articles = List.of(
            new Article("Password resets",
                    "Customers can reset a forgotten password from the sign-in page by clicking "
                            + "'Forgot password?' and following the emailed link, valid for 30 minutes. "
                            + "Free accounts are limited to 3 resets per day."),
            new Article("Shared folder access",
                    "If a customer can't access a shared folder, check: the invite may have gone to a "
                            + "different email address than they signed in with; the folder owner's account "
                            + "may be over its storage limit and read-only; a workspace admin may have "
                            + "restricted external sharing."),
            new Article("Billing and refunds",
                    "Subscriptions are billed monthly or annually. Refunds are automatic and full within "
                            + "14 days of subscribing, or any time for duplicate/erroneous charges - those "
                            + "should be escalated to a human to verify against payment records."),
            new Article("Storage limits",
                    "Free plans include 5 GB, Pro includes 2 TB, Business includes 10 TB per seat. Accounts "
                            + "over their limit become read-only for new uploads but files are never deleted."));

    @Override
    public Map<String, Object> apply(SupportGraphState state) {
        String ticket = state.ticketText().toLowerCase();
        String bestMatch = articles.stream()
                .max(Comparator.comparingInt(a -> overlapScore(ticket, a.content().toLowerCase())))
                .map(a -> "## " + a.title() + "\n" + a.content())
                .orElse("No closely matching knowledge-base article was found.");
        return Map.of(SupportGraphState.RETRIEVED_CONTEXT, bestMatch);
    }

    private int overlapScore(String ticketText, String articleText) {
        return (int) Arrays.stream(ticketText.split("\\W+"))
                .filter(word -> word.length() > 3 && articleText.contains(word))
                .count();
    }
}
