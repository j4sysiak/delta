package com.delta.bank.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@EntityListeners(AuditingEntityListener.class)
public class AccountTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String accountNumber;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column
    private String description;

    @Column(length = 64)
    private String transferRequestId;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.POSTED;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected AccountTransactionEntity() {
    }

    public AccountTransactionEntity(String accountNumber, TransactionType type, BigDecimal amount, String currency, String description) {
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransactionStatus.POSTED;
    }

    public AccountTransactionEntity(String accountNumber, TransactionType type, BigDecimal amount, String currency, String description, String transferRequestId) {
        this(accountNumber, type, amount, currency, description);
        this.transferRequestId = transferRequestId;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getTransferRequestId() {
        return transferRequestId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}