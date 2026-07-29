package com.lunawave.restaurantai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lunawave.restaurantai.dto.AskRequest;
import com.lunawave.restaurantai.dto.AskResponse;
import com.lunawave.restaurantai.service.RestaurantAskService;

@RestController
public class AskController {

    private final RestaurantAskService askService;

    public AskController(RestaurantAskService askService) {
        this.askService = askService;
    }

    @PostMapping("/api/restaurant/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return new AskResponse(
            askService.answer(request.getQuestion(), request.getRestaurantId())
        );
    }
}
