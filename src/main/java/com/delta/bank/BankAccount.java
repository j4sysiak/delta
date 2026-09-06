package com.delta.bank;

import java.math.BigDecimal;
import java.util.Objects;

public class BankAccount {
    private final String number;
    private final String owner;
    private BigDecimal balance;
    private final String currency;

    public BankAccount(String number, String owner, Money initialBalance) {
        this.number = Objects.requireNonNull(number, "number must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.currency = Objects.requireNonNull(initialBalance, "initialBalance must not be null").currency();
        this.balance = initialBalance.amount();
    }

    public String number() {
        return number;
    }

    public String owner() {
        return owner;
    }

    public Money balance() {
        return Money.of(balance, currency);
    }

    public void deposit(Money amount) {
        ensureSameCurrency(amount);
        balance = balance.add(amount.amount());
    }

    public void withdraw(Money amount) {
        ensureSameCurrency(amount);
        if (balance.compareTo(amount.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        balance = balance.subtract(amount.amount());
    }

    public void transferTo(BankAccount destination, Money amount) {
        withdraw(amount);
        destination.deposit(amount);
    }

    private void ensureSameCurrency(Money amount) {
        if (!currency.equalsIgnoreCase(amount.currency())) {
            throw new IllegalArgumentException("Currency mismatch: expected " + currency + " but got " + amount.currency());
        }
    }
}
