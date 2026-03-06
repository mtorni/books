package com.lunawave.restaurantai.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(5)
                .build()
        );

        if (docs.isEmpty()) {
            System.out.println("DOC: No matching documents found from vector search.");
        } else {
            docs.forEach(d -> {
                System.out.println("----- RAW DOC -----");
                System.out.println("TEXT: " + d.getText());
                System.out.println("META: " + safeMeta(d));
            });
        }

        List<Document> filtered = docs;

        if (restaurantId != null && !restaurantId.isBlank()) {
            String rid = restaurantId.trim();
            filtered = filtered.stream()
                .filter(d -> rid.equalsIgnoreCase(String.valueOf(safeMeta(d).get("restaurantId"))))
                .toList();
        }

        if (filtered.isEmpty()) {
            return "I’m sorry, I couldn’t find any restaurant information for that question.";
        }

        String context = filtered.stream()
            .map(this::toContextBlock)
            .collect(Collectors.joining("\n\n---\n\n"));

        String system = """
            You are a friendly and welcoming restaurant host.

            Your job is to help guests with questions about:
            - menu items
            - hours
            - specials
            - restaurant policies

            Be upbeat, warm, and conversational.
            Keep answers short and easy to read.

            IMPORTANT RULES:
            - Use ONLY the provided CONTEXT to answer.
            - Do not combine details from different menu items unless the context clearly shows they belong together.
            - When mentioning menu items, use the item names exactly as written in the context.
            - If the answer is not in the context, say you’re not sure and offer to check with the staff.
            """;

        String user = """
            GUEST QUESTION:
            %s

            CONTEXT:
            %s
            """.formatted(q, context);

        return chatClient
            .prompt()
            .system(system)
            .user(user)
            .call()
            .content();
    }

    private String toContextBlock(Document d) {
        Map<String, Object> meta = safeMeta(d);

        return """
            DOC_ID: %s
            TITLE: %s
            TYPE: %s
            TEXT: %s
            METADATA: %s
            """.formatted(
            meta.getOrDefault("docId", ""),
            meta.getOrDefault("title", ""),
            meta.getOrDefault("type", ""),
            d.getText(),
            meta
        );
    }

    private static Map<String, Object> safeMeta(Document d) {
        return Optional.ofNullable(d.getMetadata()).orElse(Map.of());
    }
}