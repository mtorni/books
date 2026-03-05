package com.lunawave.restaurantai.db;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurant_document")
public class RestaurantDocument {

    @EmbeddedId
    private RestaurantDocumentId id;

    @Column(name = "doc_type", length = 32, nullable = false)
    private String docType;

    @Column(name = "title", length = 255)
    private String title;

    @Lob
    @Column(name = "text", nullable = false, columnDefinition = "LONGTEXT")
    private String text;

    // Keep it simple: store JSON as a String for now (we’ll parse with Jackson when needed)
    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected RestaurantDocument() {}

    public RestaurantDocument(RestaurantDocumentId id, String docType, String title, String text, String metadataJson) {
        this.id = id;
        this.docType = docType;
        this.title = title;
        this.text = text;
        this.metadataJson = metadataJson;
    }

    public RestaurantDocumentId getId() { return id; }
    public String getDocType() { return docType; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getMetadataJson() { return metadataJson; }
    public String getContentHash() { return contentHash; }

    public void setDocType(String docType) { this.docType = docType; }
    public void setTitle(String title) { this.title = title; }
    public void setText(String text) { this.text = text; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}