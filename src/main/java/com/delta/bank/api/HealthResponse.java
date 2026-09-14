package com.delta.bank.api;

public record HealthResponse(
        String status,
        String service
) {
}