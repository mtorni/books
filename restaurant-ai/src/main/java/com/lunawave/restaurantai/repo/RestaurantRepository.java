package com.lunawave.restaurantai.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lunawave.restaurantai.db.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, String> {}