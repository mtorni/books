package com.lunawave.restaurantai.db;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "restaurant")
public class Restaurant {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "status", length = 32, nullable = false)
    private String status = "ACTIVE";

    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Restaurant() {}

    public Restaurant(String id, String name) {
        this.id = id;
        this.name = name;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Long getOwnerUserId() { return ownerUserId; }

    public void setName(String name) { this.name = name; }
    public void setStatus(String status) { this.status = status; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
}
