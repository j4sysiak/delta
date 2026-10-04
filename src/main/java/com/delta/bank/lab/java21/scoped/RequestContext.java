package com.delta.bank.lab.java21.scoped;

import java.util.Objects;

// Typ Rekord reprezentujący kontekst żądania: identyfikator korelacji i nazwę użytkownika.
// W tym przypadku RequestContext automatycznie dostaje pola, akcesory, equals(), hashCode() i toString(),
// a niżej zdefiniowany konstruktor kompaktowy pilnuje walidacji danych wejściowych.
public record RequestContext(

        String correlationId,
        String username
) {

    public RequestContext {
        Objects.requireNonNull(correlationId, "correlationId must not be null");
        Objects.requireNonNull(username, "username must not be null");

        if (correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }
}