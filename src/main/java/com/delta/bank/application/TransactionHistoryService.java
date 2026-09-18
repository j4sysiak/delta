package com.delta.bank.application;

import com.delta.bank.domain.AccountTransactionEntity;
import com.delta.bank.domain.AccountTransactionRepository;
import com.delta.bank.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
                                                          String to,
                                                          String minAmount,
                                                          String maxAmount) {
        LocalDateTime fromDate = from == null ? null : LocalDateTime.parse(from);
        LocalDateTime toDate = to == null ? null : LocalDateTime.parse(to);
        BigDecimal min = minAmount == null ? null : new BigDecimal(minAmount);
        BigDecimal max = maxAmount == null ? null : new BigDecimal(maxAmount);

        if (type == null && fromDate == null && toDate == null && min == null && max == null) {
            return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, pageable);
        }

        if (type != null && fromDate == null && toDate == null && min == null && max == null) {
            return repository.findByAccountNumberAndTypeOrderByCreatedAtDesc(accountNumber, type, pageable);
        }

        if (type == null && fromDate != null && toDate != null && min == null && max == null) {
            return repository.findByAccountNumberAndCreatedAtBetweenOrderByCreatedAtDesc(
                    accountNumber, fromDate, toDate, pageable
            );
        }

        if (type != null && fromDate != null && toDate != null && min == null && max == null) {
            return repository.findByAccountNumberAndTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                    accountNumber, type, fromDate, toDate, pageable
            );
        }

        if (type == null && fromDate == null && toDate == null) {
            return repository.findByAccountNumberAndAmountBetweenOrderByCreatedAtDesc(
                    accountNumber, min, max, pageable
            );
        }

        if (type != null && fromDate == null && toDate == null) {
            return repository.findByAccountNumberAndTypeAndAmountBetweenOrderByCreatedAtDesc(
                    accountNumber, type, min, max, pageable
            );
        }

        if (type == null) {
            return repository.findByAccountNumberAndCreatedAtBetweenAndAmountBetweenOrderByCreatedAtDesc(
                    accountNumber, fromDate, toDate, min, max, pageable
            );
        }

        return repository.findByAccountNumberAndTypeAndCreatedAtBetweenAndAmountBetweenOrderByCreatedAtDesc(
                accountNumber, type, fromDate, toDate, min, max, pageable
        );
    }
}