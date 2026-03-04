package com.lunawave.restaurantai.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AskController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AskController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @GetMapping("/api/restaurant/ask")
    public String ask(
        @RequestParam String q,
        @RequestParam(required = false) String restaurantId
    ) {
        // 1️⃣ Retrieve candidate docs from Chroma
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(10) // grab a few extra so filtering doesn't empty us out
                .build()
        );

        // Debug: show everything retrieved
        if (docs == null || docs.isEmpty()) {
            System.out.println("DOC: No matching documents found (before filtering).");
        } else {
            docs.forEach(d -> System.out.println("DOC (raw): " + d.getText() + " | meta=" + safeMeta(d)));
        }

        // 2️⃣ Optional restaurantId filtering (based on metadata we stored on ingest)
        List<Document> filtered = docs;
        if (restaurantId != null && !restaurantId.trim().isEmpty() && docs != null) {
            String rid = restaurantId.trim();
            filtered = docs.stream()
                .filter(d -> rid.equalsIgnoreCase(String.valueOf(safeMeta(d).get("restaurantId"))))
                .toList();
        }

        // Debug: show what's left
        if (filtered == null || filtered.isEmpty()) {
            System.out.println("DOC: No matching documents found (after restaurantId filtering). restaurantId=" + restaurantId);
        } else {
            filtered.forEach(d -> System.out.println("DOC (kept): " + d.getText()));
        }

        // 3️⃣ Build CONTEXT for the LLM
        String context = (filtered == null || filtered.isEmpty())
            ? ""
            : filtered.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4️⃣ Friendly host prompt (tone lives here)
        String system = """
            You are a friendly and welcoming restaurant host.

            Your job is to help guests with questions about:
            - menu items
            - hours
            - specials
            - restaurant policies

            Be upbeat, warm, and conversational, like a helpful host greeting guests.
            Keep answers short and easy to read.

            IMPORTANT RULES:
            - Use ONLY the provided CONTEXT to answer.
            - If the answer is not in the context, say you’re not sure and offer to check with the staff.
            """;

        String user = """
            GUEST QUESTION:
            %s

            CONTEXT (restaurant information):
            %s
            """.formatted(q, context.isBlank() ? "(no matching restaurant info found)" : context);

        // 5️⃣ Ask OpenAI with context
        return chatClient
            .prompt()
            .system(system)
            .user(user)
            .call()
            .content();
    }

    private static Map<String, Object> safeMeta(Document d) {
        Map<String, Object> meta = d.getMetadata();
        return meta == null ? Map.of() : meta;
    }
}