package com.delta.bank.api;

public record ErrorResponse(
        int status,
        String reason,
        String errorCode,
        String message
) {
}