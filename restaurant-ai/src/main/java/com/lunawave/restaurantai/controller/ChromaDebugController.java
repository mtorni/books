package com.lunawave.restaurantai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Debug / inspection controller for the Chroma vector store.
 *
 * This allows developers to quickly see what documents Chroma
 * returns for a query without going through the full AI pipeline.
 *
 * Useful for:
 * - verifying embeddings are stored correctly
 * - checking metadata values
 * - troubleshooting RAG retrieval behavior
 *
 * NOTE:
 * This is not intended for production use and should eventually
 * be protected or removed.
 */
@RestController
@RequestMapping("/api/admin/chroma")
public class ChromaDebugController {

    private final VectorStore vectorStore;

    public ChromaDebugController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Performs a similarity search directly against Chroma and
     * returns the retrieved documents with their metadata.
     *
     * Example requests:
     *
     * GET /api/admin/chroma/search?q=ribeye
     * GET /api/admin/chroma/search?q=steak&topK=20
     * GET /api/admin/chroma/search?restaurantId=harbor-steakhouse
     * GET /api/admin/chroma/search?q=ribeye&restaurantId=harbor-steakhouse
     *
     * Parameters:
     * q            - search query (optional). If omitted a default query is used.
     * restaurantId - optional metadata filter applied after retrieval.
     * topK         - number of results returned (default 10, max 50).
     */
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String restaurantId,
            @RequestParam(defaultValue = "10") int topK) {

        int safeTopK = Math.max(1, Math.min(topK, 50));

        // If no query provided, use a generic word so Chroma still returns results
        String query = (q == null || q.trim().isEmpty()) ? "restaurant" : q.trim();

        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(safeTopK)
                        .build()
        );

        // Optional filtering by restaurantId metadata
        if (restaurantId != null && !restaurantId.trim().isEmpty() && docs != null) {
            String rid = restaurantId.trim();

            docs = docs.stream()
                    .filter(d -> rid.equalsIgnoreCase(
                            String.valueOf(d.getMetadata().get("restaurantId"))))
                    .toList();
        }

        // Convert documents to simple response objects
        List<Map<String, Object>> results = (docs == null ? List.of() : docs.stream()
                .map(d -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("text", d.getText());
                    row.put("metadata", d.getMetadata());
                    return row;
                })
                .toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        resp.put("query", query);
        resp.put("restaurantId", restaurantId);
        resp.put("topK", safeTopK);
        resp.put("count", results.size());
        resp.put("results", results);

        return resp;
    }
}