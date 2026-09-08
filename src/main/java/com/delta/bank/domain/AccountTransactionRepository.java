package com.delta.bank.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransactionEntity, Long> {
    List<AccountTransactionEntity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}