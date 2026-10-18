package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * Dozwolona implementacja sealed interface BankEvent
 * reprezentująca zaksięgowaną wypłatę.

 * Jako record jest domyślnie final, więc spełnia wymagania
 * hierarchii sealed bez jawnego użycia final/sealed/non-sealed.
 */
public record WithdrawalRecorded(
        String accountNumber,
        BigDecimal amount
) implements BankEvent {

    public WithdrawalRecorded {
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