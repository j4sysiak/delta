package com.delta.bank.application;

import com.delta.bank.domain.AccountTransactionEntity;
import com.delta.bank.domain.AccountTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TransactionHistoryService {

    private final AccountTransactionRepository repository;

    public TransactionHistoryService(AccountTransactionRepository repository) {
        this.repository = repository;
    }

    public Page<AccountTransactionEntity> getTransactions(String accountNumber, Pageable pageable) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, pageable);
    }
}