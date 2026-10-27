package com.delta.bank.lab.java21.optional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccountDirectory {

    private final Map<String, AccountSummary> accountsByNumber;

    // Constructor that takes a list of AccountSummary objects
    // and creates an unmodifiable map of account numbers to AccountSummary objects.
    public AccountDirectory(List<AccountSummary> accounts) {
        Objects.requireNonNull(accounts, "accounts must not be null");

        for (var account : accounts) {
            Objects.requireNonNull(account, "account must not be null");
        }

        // Tworzy niemodyfikowalną (final) mapę:
        //   numer konta jest kluczem, a AccountSummary — wartością.
        // Jeśli numery kont się powtarzają, kolektor zgłosi IllegalStateException.
        this.accountsByNumber = accounts.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AccountSummary::accountNumber,
                        account -> account
                ));
    }

    public Optional<AccountSummary> find(String accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        AccountSummary value = accountsByNumber.get(accountNumber);
        return Optional.ofNullable(value);
    }

    public Optional<String> findOwner(String accountNumber) {
        Function<AccountSummary, String> owner = AccountSummary::owner;
        // typu lambda: Function<AccountSummary, String> ownerLambda = account -> account.owner();
        return find(accountNumber).map(owner);
    }

    /*
    @FunctionalInterface
    public interface Function<T, R> {
        R apply(T t);
    }

    Function<T, R> function = new Function<T, R>() {
        @Override
        public R apply(T t) {
            // implementation
        }
    };

    typu Lambda: Function<AccountSummary, String> ownerLambda = account -> account.owner();
    typu referencja do metody: Function<AccountSummary, String> owner = AccountSummary::owner;

     */

    public Optional<String> findAlias(String accountNumber) {
        Function<AccountSummary, Optional<? extends String>> alias = AccountSummary::alias;

        // flatMap(alias) obsługuje sytuację, gdy alias samo zwraca Optional<String> - a zwraca.
        //  - Nie znaleziono konta → Optional.empty()
        //  - Konto znalezione, ale aliasu brak → Optional.empty()
        //  - Konto i alias znalezione → Optional z aliasem
        return find(accountNumber)  // zwraca Optional<AccountSummary>
                   .flatMap(alias); // flatMap przekaże znalezione konto do alias, a potem „spłaszczy” wynik
                                    // — nie dostaniesz Optional<Optional<String>>, tylko Optional<String>.
    }

/*
    @FunctionalInterface
    public interface Function<T, R> {
      R apply(T t);
    }

    Function<T, R> function = new Function<T, R>() {
        @Override
        public R apply(T t) {
            // implementation
        }
    };

    // typu Lambda: Function<AccountSummary, Optional<? extends String>> aliasLambda = account -> account.alias();
    // typu referencja do metody: Function<AccountSummary, Optional<? extends String>> alias = AccountSummary::alias;
*/

    public AccountSummary findOrElse(
            String accountNumber,
            AccountSummary fallback
    ) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        // jeśli konto nie zostanie znalezione, zwracamy fallback, w przeciwnym razie zwracamy znalezione konto.
        return find(accountNumber).orElse(fallback);
    }

    public AccountSummary findOrElseGet(
            String accountNumber,
            Supplier<AccountSummary> fallback
    ) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        return find(accountNumber).orElseGet(fallback);
    }

    public AccountSummary findInPrimaryOrArchive(
            String accountNumber,
            AccountDirectory archive
    ) {
        Objects.requireNonNull(archive, "archive must not be null");

        return find(accountNumber)
                .or(() -> archive.find(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + accountNumber
                ));
    }

    public List<String> aliasesFor(List<String> accountNumbers) {
        Objects.requireNonNull(accountNumbers, "accountNumbers must not be null");

        return accountNumbers.stream()
                .map(this::findAlias)
                .flatMap(Optional::stream)
                .toList();
    }
}