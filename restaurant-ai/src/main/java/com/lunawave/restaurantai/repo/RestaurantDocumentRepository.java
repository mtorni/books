package com.lunawave.restaurantai.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lunawave.restaurantai.db.RestaurantDocument;
import com.lunawave.restaurantai.db.RestaurantDocumentId;

public interface RestaurantDocumentRepository extends JpaRepository<RestaurantDocument, RestaurantDocumentId> {

    List<RestaurantDocument> findByIdRestaurantId(String restaurantId);

    void deleteByIdRestaurantId(String restaurantId);
}