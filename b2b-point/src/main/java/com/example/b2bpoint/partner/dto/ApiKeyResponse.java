package com.example.b2bpoint.partner.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
public class ApiKeyResponse {

    private final Long partnerId;
    private final String apiKey;
    private final String message;

    @Builder
    public ApiKeyResponse(Long partnerId, String apiKey, String message) {
        this.partnerId = partnerId;
        this.apiKey = apiKey;
        this.message = message;
    }
}
