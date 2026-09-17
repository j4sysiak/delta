package com.delta.bank.application;

import com.delta.bank.domain.AccountTransactionEntity;
import com.delta.bank.domain.AccountTransactionRepository;
import com.delta.bank.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionHistoryService {

    private final AccountTransactionRepository repository;

    public TransactionHistoryService(AccountTransactionRepository repository) {
        this.repository = repository;
    }

    public Page<AccountTransactionEntity> getTransactions(String accountNumber,
                                                          Pageable pageable,
                                                          TransactionType type,
                                                          String from,
                                                          String to) {
        Page<AccountTransactionEntity> page;
        if (type == null) {
            page = repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, pageable);
        } else {
            page = repository.findByAccountNumberAndTypeOrderByCreatedAtDesc(accountNumber, type, pageable);
        }
        if (from == null && to == null) {
            return page;
        }
        LocalDateTime fromDate = from == null ? LocalDateTime.MIN : LocalDateTime.parse(from);
        LocalDateTime toDate = to == null ? LocalDateTime.MAX : LocalDateTime.parse(to);

        if (type == null) {
            return repository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtDesc(accountNumber, fromDate, toDate, pageable);
        }

        return repository.findByAccountNumberAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(accountNumber, type, fromDate, toDate, pageable);
    }
}