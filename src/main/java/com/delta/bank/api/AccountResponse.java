package com.delta.bank.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        String number,
        String owner,
        BigDecimal balance,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}