package com.lunawave.restaurantai.dto;

import java.util.List;

public record BulkUpsertDocsRequest(
    List<UpsertDocRequest> docs
) {}