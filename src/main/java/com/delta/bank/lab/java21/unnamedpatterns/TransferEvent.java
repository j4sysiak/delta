package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferEvent(
        String sourceAccount,
        String targetAccount,
        BigDecimal amount
) implements StatementEvent {

    public TransferEvent {
        Objects.requireNonNull(sourceAccount, "sourceAccount must not be null");
        Objects.requireNonNull(targetAccount, "targetAccount must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (sourceAccount.isBlank()) {
            throw new IllegalArgumentException("sourceAccount must not be blank");
        }
        if (targetAccount.isBlank()) {
            throw new IllegalArgumentException("targetAccount must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}