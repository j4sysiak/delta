package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Dozwolona implementacja sealed interface BankEvent
 * reprezentująca zaksięgowany przelew.

 * Jako record jest domyślnie final, więc spełnia wymagania
 * hierarchii sealed bez jawnego użycia final/sealed/non-sealed.
 */
public record TransferRecorded(
        String sourceAccount,
        String targetAccount,
        BigDecimal amount
) implements BankEvent {

    public TransferRecorded {
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