package com.delta.bank.api;

public record ErrorResponse(
        int status,
        String error,
        String message
) {
}