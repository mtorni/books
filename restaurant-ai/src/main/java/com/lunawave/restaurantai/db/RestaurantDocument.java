package com.lunawave.restaurantai.db;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
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

    // store JSON as a string for now
    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // DB will populate these automatically
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public RestaurantDocument(RestaurantDocumentId id,
                              String docType,
                              String title,
                              String text,
                              String metadataJson) {
        this.id = id;
        this.docType = docType;
        this.title = title;
        this.text = text;
        this.metadataJson = metadataJson;
    }
}
