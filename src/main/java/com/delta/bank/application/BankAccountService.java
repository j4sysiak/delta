package com.delta.bank.application;

import com.delta.bank.domain.BankAccountEntity;
import com.delta.bank.domain.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class BankAccountService {

    private final BankAccountRepository repository;

    public BankAccountService(BankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public BankAccountEntity openAccount(String number, String owner, BigDecimal balance, String currency) {
        return repository.save(new BankAccountEntity(number, owner, balance, currency));
    }

    @Transactional(readOnly = true)
    public BankAccountEntity find(String number) {
        return repository.findById(number)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));
    }
}
