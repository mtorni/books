package com.lunawave.restaurantai.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lunawave.restaurantai.dto.AskRequest;
import com.lunawave.restaurantai.dto.AskResponse;

@RestController
public class AskController {

    private static final Set<String> TAG_KEYWORDS = Set.of(
        "steak",
        "seafood",
        "lunch",
        "dinner",
        "salad",
        "policy",
        "parking",
        "hours"
    );

    private static final Pattern UNDER_PATTERN =
        Pattern.compile("\\b(?:under|below|less than)\\s*\\$?(\\d+(?:\\.\\d+)?)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern OVER_PATTERN =
        Pattern.compile("\\b(?:over|above|more than)\\s*\\$?(\\d+(?:\\.\\d+)?)\\b", Pattern.CASE_INSENSITIVE);

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AskController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    @PostMapping("/api/restaurant/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        String q = request.getQuestion();
        String restaurantId = request.getRestaurantId();

        if (q == null || q.isBlank()) {
            return new AskResponse("Please enter a question.");
        }

        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(q)
                .topK(8)
                .build()
        );

        if (docs.isEmpty()) {
            return new AskResponse("I’m sorry, I couldn’t find any restaurant information for that question.");
        }

        List<Document> filtered = docs;

        if (restaurantId != null && !restaurantId.isBlank()) {
            String rid = restaurantId.trim();
            filtered = filtered.stream()
                .filter(d -> rid.equalsIgnoreCase(String.valueOf(safeMeta(d).get("restaurantId"))))
                .toList();
        }

        if (filtered.isEmpty()) {
            return new AskResponse("I’m sorry, I couldn’t find any restaurant information for that question.");
        }

        PriceFilter priceFilter = extractPriceFilter(q);
        if (priceFilter != null) {
            filtered = applyPriceFilter(filtered, priceFilter);
        }

        Set<String> requestedTags = extractRequestedTags(q);
        if (!requestedTags.isEmpty()) {
            filtered = applyTagFilter(filtered, requestedTags);
        }

        if (filtered.isEmpty()) {
            return new AskResponse("I’m sorry, I couldn’t find any restaurant information for that question.");
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
            - Only mention items that appear in the filtered CONTEXT.
            - Do not change, reformat, reinterpret, or "fix" any dollar amount written by the guest.
            - If the guest says $4190, treat it as $4190 exactly, not $41.90.
            - When filters have already been applied, do not restate the filter amount unless truly necessary.
            - Prefer simply listing the matching items from the CONTEXT rather than paraphrasing the filter.
            - If the answer is not in the context, say you’re not sure and offer to check with the staff.
            """;

        String user = """
            GUEST QUESTION:
            %s

            CONTEXT:
            %s
            """.formatted(q, context);

        String answer = chatClient
            .prompt()
            .system(system)
            .user(user)
            .call()
            .content();

        return new AskResponse(answer);
    }

    private List<Document> applyPriceFilter(List<Document> docs, PriceFilter priceFilter) {
        return docs.stream()
            .filter(d -> {
                BigDecimal price = readPrice(d);
                if (price == null) {
                    return false;
                }

                return switch (priceFilter.operator()) {
                    case MAX -> price.compareTo(priceFilter.amount()) < 0;
                    case MIN -> price.compareTo(priceFilter.amount()) > 0;
                };
            })
            .toList();
    }

    private List<Document> applyTagFilter(List<Document> docs, Set<String> requestedTags) {
        return docs.stream()
            .filter(d -> matchesAnyRequestedTag(d, requestedTags))
            .toList();
    }

    private boolean matchesAnyRequestedTag(Document d, Set<String> requestedTags) {
        Map<String, Object> meta = safeMeta(d);

        String type = String.valueOf(meta.getOrDefault("type", "")).toLowerCase();
        String title = String.valueOf(meta.getOrDefault("title", "")).toLowerCase();
        String text = d.getText() == null ? "" : d.getText().toLowerCase();
        String tags = String.valueOf(meta.getOrDefault("meta_tags", "")).toLowerCase();

        for (String requestedTag : requestedTags) {
            if (type.contains(requestedTag)
                || title.contains(requestedTag)
                || text.contains(requestedTag)
                || tags.contains(requestedTag)) {
                return true;
            }

            if ("hours".equals(requestedTag) && "hours".equals(type)) {
                return true;
            }

            if ("policy".equals(requestedTag) && "policy".equals(type)) {
                return true;
            }
        }

        return false;
    }

    private Set<String> extractRequestedTags(String q) {
        String lower = q.toLowerCase();

        return TAG_KEYWORDS.stream()
            .filter(lower::contains)
            .collect(Collectors.toSet());
    }

    private PriceFilter extractPriceFilter(String q) {
        Matcher underMatcher = UNDER_PATTERN.matcher(q);
        if (underMatcher.find()) {
            return new PriceFilter(
                PriceOperator.MAX,
                new BigDecimal(underMatcher.group(1))
            );
        }

        Matcher overMatcher = OVER_PATTERN.matcher(q);
        if (overMatcher.find()) {
            return new PriceFilter(
                PriceOperator.MIN,
                new BigDecimal(overMatcher.group(1))
            );
        }

        return null;
    }

    private BigDecimal readPrice(Document d) {
        Object raw = safeMeta(d).get("meta_price");
        if (raw == null) {
            return null;
        }

        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (Exception e) {
            return null;
        }
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

    private enum PriceOperator {
        MAX,
        MIN
    }

    private record PriceFilter(PriceOperator operator, BigDecimal amount) {
    }
}