package com.lunawave.restaurantai.controller;

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

    @PostMapping("/docs")
    public String addDocument(@RequestBody Map<String, String> body) {

        String text = body.get("text");

        Document doc = new Document(text);

        vectorStore.add(java.util.List.of(doc));

        return "Document stored successfully";
    }
}