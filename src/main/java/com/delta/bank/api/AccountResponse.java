package com.delta.bank.api;

import java.math.BigDecimal;

public record AccountResponse(
        String number,
        String owner,
        BigDecimal balance,
        String currency
) {
}