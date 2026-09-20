package com.delta.bank.lab.java21.concurrency;

import java.math.BigDecimal;
import java.util.Objects;

public class UnsafeBankAccount implements DepositAccount {

    private BigDecimal balance = BigDecimal.ZERO;

    @Override
    public void /* synchronized */ deposit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        BigDecimal currentBalance = balance;

        /*
         * Ten yield jest użyty wyłącznie edukacyjnie.
         * Zwiększa szansę przełączenia wątku pomiędzy odczytem i zapisem.
         * Nie zapewnia bezpieczeństwa współbieżnego.
         */
        Thread.yield();

        // Ta klasa może tracić aktualizacje, ponieważ to operacja:
        // nie jest chroniona sekcją krytyczną synchronized,
        // która zapewnia, że w danym momencie tylko jeden wątek wykonuje deposit dla tego konta..
        balance = currentBalance.add(amount);
    }

    @Override
    public BigDecimal balance() {
        return balance;
    }
}