package com.delta.bank.application;

import com.delta.bank.domain.AccountTransactionEntity;
import com.delta.bank.domain.AccountTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionHistoryService {

    private final AccountTransactionRepository repository;

    public TransactionHistoryService(AccountTransactionRepository repository) {
        this.repository = repository;
    }

    public List<AccountTransactionEntity> getTransactions(String accountNumber) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }
}