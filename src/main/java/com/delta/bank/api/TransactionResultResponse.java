package com.delta.bank.api;

import java.math.BigDecimal;

public record TransactionResultResponse(
        boolean executed,
        String requestId,
        String accountNumber,
        BigDecimal balance,
        BigDecimal amount
) {
}
