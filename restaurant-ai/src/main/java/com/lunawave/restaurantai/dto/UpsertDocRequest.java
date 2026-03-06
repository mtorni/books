package com.lunawave.restaurantai.dto;

public record UpsertDocRequest(
    String docId,
    String type,
    String title,
    String text,
    String metadataJson
) {}
