package com.lunawave.restaurantai.controller;

import java.util.List;
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
    public String ask(@RequestParam String q) {

        // 1️) Retrieve relevant restaurant docs from Chroma
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(6)
                .build()
        );

        // 2️) Debug: print what Chroma returned
        if (docs != null && !docs.isEmpty()) {
            docs.forEach(d -> System.out.println("DOC: " + d.getText()));
        } else {
            System.out.println("DOC: No matching documents found.");
        }

        // 3️) Build context string from retrieved docs
        String context = (docs == null || docs.isEmpty())
            ? ""
            : docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 4️) Friendly host system prompt
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

        // 5️)User prompt including retrieved context
        String user = """
            GUEST QUESTION:
            %s

            CONTEXT (restaurant information):
            %s
            """.formatted(q, context.isBlank() ? "(no matching restaurant info found)" : context);

        // 6️) Ask OpenAI using retrieved context
        return chatClient
            .prompt()
            .system(system)
            .user(user)
            .call()
            .content();
    }
}