package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record DepositEvent(String accountNumber, BigDecimal amount)
        implements StatementEvent {

    public DepositEvent {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}