package com.lunawave.restaurantai.db;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RestaurantDocumentId implements Serializable {

    @Column(name = "restaurant_id", length = 64, nullable = false)
    private String restaurantId;

    @Column(name = "doc_id", length = 128, nullable = false)
    private String docId;

    protected RestaurantDocumentId() {}

    public RestaurantDocumentId(String restaurantId, String docId) {
        this.restaurantId = restaurantId;
        this.docId = docId;
    }

    public String getRestaurantId() { return restaurantId; }
    public String getDocId() { return docId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RestaurantDocumentId other)) return false;
        return Objects.equals(restaurantId, other.restaurantId)
            && Objects.equals(docId, other.docId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restaurantId, docId);
    }
}