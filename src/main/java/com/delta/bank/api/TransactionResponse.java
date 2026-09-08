package com.delta.bank.api;

import com.delta.bank.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String accountNumber,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        LocalDateTime createdAt
) {
}