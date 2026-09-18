package com.delta.bank.api.dto;

import com.delta.bank.domain.TransactionStatus;
import com.delta.bank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/*
To jest DTO (Data Transfer Object), ponieważ:
 - przenosi dane między warstwami aplikacji,
 - nie zawiera logiki biznesowej,
 - reprezentuje odpowiedź API (TransactionResponse).

W tym przypadku jest to typ "record" w Javie bardzo dobrze pasuje do DTO, bo daje:
 - niemutowalność, czyli pola są finalne i nie można ich zmienić po utworzeniu obiektu,
 - zwięzłą definicję,
 - automatycznie wygenerowane accessory, equals(), hashCode() i toString().
* */
public record TransactionResponse(
        Long id,
        String accountNumber,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        String transferRequestId,
        TransactionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}