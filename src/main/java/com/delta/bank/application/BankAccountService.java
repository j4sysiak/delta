package com.delta.bank.application;

import com.delta.bank.domain.AccountTransactionEntity;
import com.delta.bank.domain.AccountTransactionRepository;
import com.delta.bank.domain.BankAccountEntity;
import com.delta.bank.domain.BankAccountRepository;
import com.delta.bank.domain.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BankAccountService {

    private final BankAccountRepository repository;
    private final AccountTransactionRepository transactionRepository;

    public BankAccountService(BankAccountRepository repository, AccountTransactionRepository transactionRepository) {
        this.repository = repository;
        this.transactionRepository = transactionRepository;
    }

    // Warto dodać @Transactional — dla Optimistic Locking: @Transactional + @Version działa razem.
    @Transactional
    public BankAccountEntity openAccount(String number, String owner, BigDecimal balance, String currency) {
        validate(number, owner, balance, currency);

        if (repository.existsById(number)) {
            throw new IllegalArgumentException("Account already exists: " + number);
        }

        BankAccountEntity account = repository.save(new BankAccountEntity(number, owner, balance, currency));

        transactionRepository.save(new AccountTransactionEntity(
                account.getNumber(),
                TransactionType.DEPOSIT,
                account.getBalance(),
                account.getCurrency(),
                "Account opened"
        ));

        return account;
    }

    // Warto dodać @Transactional — dla Optimistic Locking: @Transactional + @Version działa razem.
    // W tej operacji używamy dodatkowo oprócz Optimistic Locking także PESSIMISTIC_WRITE lock (FOR UPDATE)
    // w repository.findAllForUpdateOrderByNumber(orderedNumbers)
    @Transactional
    public void transfer(String fromNumber, String toNumber, BigDecimal amount) {
        validateAmount(amount);

        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        List<String> orderedNumbers = List.of(fromNumber, toNumber).stream()
                .sorted()
                .toList();

        /*
        Ten fragment:
        1. wywołuje repository.findAllForUpdateOrderByNumber(orderedNumbers)
        2. pobiera konta dla numerów z orderedNumbers
        3. zakłada na nich blokadę na czas transakcji (FOR UPDATE)
        4. sortuje/pobiera je w ustalonej kolejności, żeby zmniejszyć ryzyko deadlocków
        5. zamienia wynik na Map<String, BankAccountEntity>, gdzie:
              - kluczem jest numer konta (BankAccountEntity::getNumber)
              - wartością jest obiekt BankAccountEntity
        * */
        Map<String, BankAccountEntity> lockedAccounts =
                repository.findAllForUpdateOrderByNumber(orderedNumbers).stream()
                        .collect(Collectors.toMap(BankAccountEntity::getNumber, account -> account));

        // lockedAccounts.get(fromNumber) daje zablokowane konto źródłowe
        BankAccountEntity from = lockedAccounts.get(fromNumber);

        if (from == null) {
            throw new IllegalArgumentException("Source account not found: " + fromNumber);
        }

        // lockedAccounts.get(toNumber) daje zablokowane konto docelowe.
        BankAccountEntity to = lockedAccounts.get(toNumber);

        if (to == null) {
            throw new IllegalArgumentException("Destination account not found: " + toNumber);
        }

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new IllegalArgumentException("Currency mismatch between accounts");
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds for transfer");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        repository.save(from);
        repository.save(to);

        transactionRepository.save(new AccountTransactionEntity(
                from.getNumber(),
                TransactionType.TRANSFER_OUT,
                amount,
                from.getCurrency(),
                "Transfer to " + to.getNumber()
        ));

        transactionRepository.save(new AccountTransactionEntity(
                to.getNumber(),
                TransactionType.TRANSFER_IN,
                amount,
                to.getCurrency(),
                "Transfer from " + from.getNumber()
        ));
    }

    // Warto dodać @Transactional — dla Optimistic Locking: @Transactional + @Version działa razem.
    @Transactional
    public BankAccountEntity deposit(String number, BigDecimal amount) {
        validateAmount(amount);

        BankAccountEntity account = repository.findById(number)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));

        account.setBalance(account.getBalance().add(amount));
        repository.save(account);

        transactionRepository.save(new AccountTransactionEntity(
                account.getNumber(),
                TransactionType.DEPOSIT,
                amount,
                account.getCurrency(),
                "Deposit"
        ));

        return account;
    }

    // Warto dodać @Transactional — dla Optimistic Locking: @Transactional + @Version działa razem.
    @Transactional
    public BankAccountEntity withdraw(String number, BigDecimal amount) {
        validateAmount(amount);

        BankAccountEntity account = repository.findById(number)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        account.setBalance(account.getBalance().subtract(amount));
        repository.save(account);

        transactionRepository.save(new AccountTransactionEntity(
                account.getNumber(),
                TransactionType.WITHDRAW,
                amount,
                account.getCurrency(),
                "Withdrawal"
        ));

        return account;
    }

    @Transactional(readOnly = true)
    public BankAccountEntity find(String number) {
        return repository.findById(number)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + number));
    }

    private void validate(String number, String owner, BigDecimal balance, String currency) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner is required");
        }
        if (balance == null) {
            throw new IllegalArgumentException("Balance is required");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency must be a 3-letter uppercase code");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}