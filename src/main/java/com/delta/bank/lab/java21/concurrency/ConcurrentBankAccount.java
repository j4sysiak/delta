package com.delta.bank.lab.java21.concurrency;

import java.math.BigDecimal;
import java.util.Objects;

public class ConcurrentBankAccount implements DepositAccount {

    private BigDecimal balance = BigDecimal.ZERO;

    // synchronized zapewnia, że w danym momencie tylko jeden wątek wykonuje deposit dla tego konta.
    @Override
    public synchronized void deposit(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        balance = balance.add(amount);
    }

    @Override
    public synchronized BigDecimal balance() {
        return balance;
    }
}