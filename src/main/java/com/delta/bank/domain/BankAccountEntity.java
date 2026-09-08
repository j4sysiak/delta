package com.delta.bank.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "bank_accounts")
public class BankAccountEntity {

    @Id
    @Column(nullable = false, length = 64)
    private String number;

    @Column(nullable = false, length = 128)
    private String owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    protected BankAccountEntity() {
    }

    public BankAccountEntity(String number, String owner, BigDecimal balance, String currency) {
        this.number = Objects.requireNonNull(number);
        this.owner = Objects.requireNonNull(owner);
        this.balance = Objects.requireNonNull(balance);
        this.currency = Objects.requireNonNull(currency);
    }

    public String getNumber() {
        return number;
    }

    public String getOwner() {
        return owner;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
