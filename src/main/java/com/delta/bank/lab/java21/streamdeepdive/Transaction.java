package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(
        String id,
        String accountNumber,
        TransactionType type,
        BigDecimal amount
) {

    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}