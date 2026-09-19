package com.delta.bank.lab.java21.recordpatterns;

import java.math.BigDecimal;

public record Money(
        BigDecimal amount,
        String currency
) {
}