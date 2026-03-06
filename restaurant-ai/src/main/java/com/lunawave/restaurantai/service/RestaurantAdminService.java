package com.lunawave.restaurantai.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunawave.restaurantai.db.RestaurantDocument;
import com.lunawave.restaurantai.db.RestaurantDocumentId;
import com.lunawave.restaurantai.dto.UpsertDocRequest;
import com.lunawave.restaurantai.repo.RestaurantDocumentRepository;

@Service
public class RestaurantAdminService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RestaurantDocumentRepository docRepo;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    public RestaurantAdminService(
        RestaurantDocumentRepository docRepo,
        VectorStore vectorStore,
        ObjectMapper objectMapper
    ) {
        this.docRepo = docRepo;
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    public List<RestaurantDocument> listDocs(String restaurantId) {
        return docRepo.findByIdRestaurantId(restaurantId);
    }

    public Optional<RestaurantDocument> getDoc(String restaurantId, String docId) {
        return docRepo.findById(new RestaurantDocumentId(restaurantId, docId));
    }

    @Transactional
    public List<String> upsertDocs(String restaurantId, List<UpsertDocRequest> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        return docs.stream()
            .map(this::requireValidDoc)
            .map(doc -> upsertDoc(
                restaurantId,
                doc.docId(),
                doc.type(),
                doc.title(),
                doc.text(),
                doc.metadataJson()
            ))
            .map(saved -> saved.getId().getDocId())
            .toList();
    }

    @Transactional
    public RestaurantDocument upsertDoc(
        String restaurantId,
        String docId,
        String type,
        String title,
        String text,
        String metadataJson
    ) {
        validateRequired("restaurantId", restaurantId);
        validateRequired("docId", docId);
        validateRequired("type", type);
        validateRequired("title", title);
        validateRequired("text", text);

        String normalizedRestaurantId = restaurantId.trim();
        String normalizedDocId = docId.trim();
        String normalizedType = type.trim();
        String normalizedTitle = title.trim();
        String normalizedText = text.trim();
        String normalizedMetadataJson = normalizeMetadataJson(metadataJson);

        var id = new RestaurantDocumentId(normalizedRestaurantId, normalizedDocId);

        var existing = docRepo.findById(id);
        RestaurantDocument doc = existing.orElseGet(() ->
            new RestaurantDocument(id, normalizedType, normalizedTitle, normalizedText, normalizedMetadataJson)
        );

        doc.setDocType(normalizedType);
        doc.setTitle(normalizedTitle);
        doc.setText(normalizedText);
        doc.setMetadataJson(normalizedMetadataJson);

        // Optional content hash if your entity supports it.
        trySetContentHash(doc, buildContentHash(normalizedType, normalizedTitle, normalizedText, normalizedMetadataJson));

        RestaurantDocument saved = docRepo.save(doc);

        String vectorId = toVectorDocId(normalizedRestaurantId, normalizedDocId);

        // Idempotent upsert for vector store:
        // delete old vector doc, then add fresh one using the same stable ID.
        vectorStore.delete(List.of(vectorId));
        vectorStore.add(List.of(toVectorDocument(saved)));

        return saved;
    }

    @Transactional
    public void deleteDoc(String restaurantId, String docId) {
        validateRequired("restaurantId", restaurantId);
        validateRequired("docId", docId);

        String normalizedRestaurantId = restaurantId.trim();
        String normalizedDocId = docId.trim();

        docRepo.deleteById(new RestaurantDocumentId(normalizedRestaurantId, normalizedDocId));
        vectorStore.delete(List.of(toVectorDocId(normalizedRestaurantId, normalizedDocId)));
    }

    @Transactional
    public void deleteAllDocs(String restaurantId) {
        validateRequired("restaurantId", restaurantId);

        String normalizedRestaurantId = restaurantId.trim();

        List<String> vectorIds = docRepo.findByIdRestaurantId(normalizedRestaurantId).stream()
            .map(d -> toVectorDocId(d.getId().getRestaurantId(), d.getId().getDocId()))
            .toList();

        docRepo.deleteByIdRestaurantId(normalizedRestaurantId);

        if (!vectorIds.isEmpty()) {
            vectorStore.delete(vectorIds);
        }
    }

    private UpsertDocRequest requireValidDoc(UpsertDocRequest doc) {
        if (doc == null) {
            throw new IllegalArgumentException("Each docs[] item is required");
        }
        validateRequired("docId", doc.docId());
        validateRequired("type", doc.type());
        validateRequired("title", doc.title());
        validateRequired("text", doc.text());
        return doc;
    }

    private void validateRequired(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    private Document toVectorDocument(RestaurantDocument doc) {
        String restaurantId = doc.getId().getRestaurantId();
        String docId = doc.getId().getDocId();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("restaurantId", restaurantId);
        metadata.put("docId", docId);
        metadata.put("title", doc.getTitle());
        metadata.put("type", doc.getDocType());

        Map<String, Object> parsedMetadata = parseMetadataJson(doc.getMetadataJson());
        flattenInto(metadata, "meta_", parsedMetadata);

        String vectorText = """
            TITLE: %s
            TYPE: %s
            TEXT: %s
            """.formatted(
            nullSafe(doc.getTitle()),
            nullSafe(doc.getDocType()),
            nullSafe(doc.getText())
        );

        return new Document(
            toVectorDocId(restaurantId, docId),
            vectorText,
            metadata
        );
    }

    private String toVectorDocId(String restaurantId, String docId) {
        return restaurantId + "::" + docId;
    }

    private String normalizeMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return "{}";
        }

        try {
            Object parsed = objectMapper.readValue(metadataJson, Object.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            throw new IllegalArgumentException("metadataJson must be valid JSON", e);
        }
    }

    private Map<String, Object> parseMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("metadataJson must be a JSON object", e);
        }
    }

    /**
     * Spring AI metadata should use simple values.
     * Nested objects and arrays are flattened/stringified so they remain portable.
     */
    private void flattenInto(Map<String, Object> target, String prefix, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = prefix + entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                target.put(key, "");
            } else if (isSimpleValue(value)) {
                target.put(key, value);
            } else {
                target.put(key, writeJsonSilently(value));
            }
        }
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof String
            || value instanceof Number
            || value instanceof Boolean;
    }

    private String writeJsonSilently(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String buildContentHash(String type, String title, String text, String metadataJson) {
        String raw = String.join("||",
            nullSafe(type),
            nullSafe(title),
            nullSafe(text),
            nullSafe(metadataJson)
        );

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private void trySetContentHash(RestaurantDocument doc, String contentHash) {
        try {
            var method = doc.getClass().getMethod("setContentHash", String.class);
            method.invoke(doc, contentHash);
        } catch (NoSuchMethodException e) {
            // Entity does not expose setContentHash yet; ignore.
        } catch (Exception e) {
            throw new IllegalStateException("Failed setting contentHash on RestaurantDocument", e);
        }
    }

    private String nullSafe(String value) {
        return Objects.toString(value, "");
    }
}