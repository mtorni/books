package com.lunawave.restaurantai.controller;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurant")
public class DocumentController {

    private final VectorStore vectorStore;

    public DocumentController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Ingest a restaurant knowledge document into the vector store (Chroma).
     *
     * Postman example:
     * POST http://localhost:8080/api/restaurant/docs
     *
     * Body:
     * {
     *   "restaurantId": "harbor-steakhouse",
     *   "id": "hours-main",
     *   "type": "hours",
     *   "text": "Hours: Mon–Thu 4–10pm, Fri–Sat 4–11pm, Sun closed.",
     *   "metadata": {
     *     "source": "hours.json"
     *   }
     * }
     */
    @PostMapping("/docs")
    public Map<String, Object> addDocument(@RequestBody IngestRequest req) {

        if (req == null || isBlank(req.text())) {
            return Map.of(
                "ok", false,
                "message", "Missing required field: text"
            );
        }

        // Merge metadata:
        // - top-level fields become metadata too (useful for filtering later)
        // - user-supplied metadata is included
        Map<String, Object> meta = new HashMap<>();
        if (req.metadata() != null) {
            meta.putAll(req.metadata());
        }

        if (!isBlank(req.restaurantId())) meta.put("restaurantId", req.restaurantId());
        if (!isBlank(req.type())) meta.put("type", req.type());

        // Helpful debug/auditing metadata
        meta.put("ingestedAt", OffsetDateTime.now().toString());

        // Create Document.
        // In Spring AI 2.x, Document text is the main searchable content.
        Document doc = new Document(req.text(), meta);

        // NOTE: The Document id may or may not be used depending on the vector store implementation.
        // If you need deterministic updates later, we can implement "delete then add" using metadata filters.
        vectorStore.add(List.of(doc));

        return Map.of(
            "ok", true,
            "message", "Document stored successfully",
            "stored", Map.of(
                "id", req.id(),
                "type", req.type(),
                "restaurantId", req.restaurantId(),
                "metadataKeys", meta.keySet()
            )
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Request DTO
    public record IngestRequest(
        String restaurantId,
        String id,
        String type,
        String text,
        Map<String, Object> metadata
    ) {}
}