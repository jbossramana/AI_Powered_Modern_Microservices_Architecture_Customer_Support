
package com.example.support.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class WebsiteIngestionService {

    // Stores website content as vector embeddings for later retrieval.
    private final VectorStore vectorStore;

    // HTTP client used to download website pages.
    private final HttpClient httpClient;

    // Splits extracted website text into smaller chunks.
    private final TokenTextSplitter textSplitter =
            new TokenTextSplitter();

    public WebsiteIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;

        // Configure HTTP/1.1, follow redirects, and set connection timeout.
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    // Entry point used by the existing SupportController.
    public int indexPages(List<String> urls) {
        return ingestConfiguredPages(urls);
    }

    // Downloads, extracts, chunks, and indexes configured website pages.
    public int ingestConfiguredPages(List<String> urls) {

        // Validate that at least one URL is configured.
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException(
                    "No website URLs configured.");
        }

        int totalChunks = 0;

        // Process each configured website URL.
        for (String url : urls) {
            try {
                // Create an HTTP GET request to download the page.
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(45))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                // Send the request and receive the HTML response.
                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

                // Stop processing if the website returns an unsuccessful status.
                if (response.statusCode() < 200
                        || response.statusCode() >= 300) {
                    throw new IllegalStateException(
                            "HTTP status: " + response.statusCode());
                }

                // Parse the downloaded HTML using Jsoup.
                var html = Jsoup.parse(
                        response.body(),
                        response.uri().toString());

                // Extract the webpage title.
                String title = html.title();

                // Remove scripts, navigation, and other unwanted elements.
                html.select(
                        "script, style, noscript, iframe, svg, " +
                        "nav, footer, header, aside")
                        .remove();

                // Extract readable text from the HTML body.
                String text = html.body().text().trim();

                // Skip pages that contain no readable text.
                if (text.isBlank()) {
                    continue;
                }

                // Attach source URL and title as document metadata.
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", response.uri().toString());
                metadata.put("title", title);

                // Create a Spring AI document containing the page text.
                Document document = new Document(text, metadata);

                // Split the page text into smaller chunks for retrieval.
                List<Document> chunks =
                        textSplitter.split(List.of(document));

                // Generate embeddings and save the chunks in the vector store.
                vectorStore.add(chunks);

                // Update the total number of indexed chunks.
                totalChunks += chunks.size();

                System.out.println(
                        "Indexed " + chunks.size() +
                        " chunks from " + url);

            } catch (Exception e) {
                // Report the URL that failed and preserve the original cause.
                throw new IllegalStateException(
                        "Ingestion failed for " + url + ": " +
                        e.getMessage(),
                        e);
            }
        }

        // Log the total number of chunks indexed across all URLs.
        System.out.println("Total indexed chunks: " + totalChunks);

        // Return the total count to the controller.
        return totalChunks;
    }
}
