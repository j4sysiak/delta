package com.delta.bank.lab.java21.optional

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class AccountDirectorySpec extends Specification {

    def "returns a present Optional when account exists"() {
        when:
        // result jest typu Optional<AccountSummary>, ponieważ metoda find(...) zwraca Optional<AccountSummary>.
        // dzięki temu możemy użyć metod isPresent() i get() do sprawdzenia, czy konto istnieje i pobrania jego szczegółów.
        def result = directory().find("PLN-1001")

        then:
        result.isPresent()
        result.get().owner() == "Anna Kowalska"
    }

    def "returns an empty Optional when account does not exist"() {
        expect:
        directory().find("PLN-9999-superfake").isEmpty()
    }

    def "map transforms a present value and leaves an empty result empty"() {
        expect:
        directory().findOwner("PLN-1001") == Optional.of("Anna Kowalska")
        directory().findOwner("PLN-9999-superfake").isEmpty()
    }

    def "flatMap avoids a nested Optional when alias exists"() {
        expect:
        directory().findAlias("PLN-1001") == Optional.of("anna-main")
    }

    def "flatMap returns empty when account has no alias"() {
        expect:
        directory().findAlias("PLN-1002").isEmpty()
    }

    def "orElse returns fallback for a missing account"() {
        given:
        def fallback = new AccountSummary(
                "UNKNOWN",
                "Unknown owner",
                Optional.empty()
        )

        expect:
        // Ta linia sprawdza, czy dla nieistniejącego konta PLN-9999 metoda findOrElse(...)
        // zwraca dokładnie ten sam obiekt fallback, który przekazano jako argument.
        // W Groovy .is(fallback) porównuje tożsamość obiektów, a nie tylko ich zawartość.
        // Test potwierdza więc, że gdy konto nie zostanie znalezione, metoda używa podanej wartości zastępczej.
        directory().findOrElse("PLN-9999", fallback).is(fallback)
    }

    def "orElseGet evaluates fallback only when value is absent"() {
        given:
        def fallbackCalls = new AtomicInteger()
        def fallback = {
            fallbackCalls.incrementAndGet()
            new AccountSummary("FALLBACK", "Fallback owner", Optional.empty())
        }

        when:
        def found = directory().findOrElseGet("PLN-1001", fallback)

        then:
        found.accountNumber() == "PLN-1001"
        fallbackCalls.get() == 0

        when:
        def missing = directory().findOrElseGet("PLN-9999", fallback)

        then:
        missing.accountNumber() == "FALLBACK"
        fallbackCalls.get() == 1
    }

    def "orElse evaluates its argument even when Optional contains a value"() {
        given:
        def fallbackCalls = new AtomicInteger()
        def fallback = {
            fallbackCalls.incrementAndGet()
            new AccountSummary("FALLBACK", "Fallback owner", Optional.empty())
        }

        when:
        def found = directory().find("PLN-1001")
                .orElse(fallback.call())

        then:
        found.accountNumber() == "PLN-1001"
        fallbackCalls.get() == 1
    }

    def "or tries archive only when primary lookup is empty"() {
        given:
        def primary = directory()
        def archive = new AccountDirectory([
                new AccountSummary(
                        "PLN-2001",
                        "Archived owner",
                        Optional.empty()
                )
        ])

        expect:
        primary.findInPrimaryOrArchive("PLN-1001", archive).owner() ==
                "Anna Kowalska"
        primary.findInPrimaryOrArchive("PLN-2001", archive).owner() ==
                "Archived owner"
    }

    def "orElseThrow reports a missing account"() {
        when:
        directory().findInPrimaryOrArchive(
                "PLN-9999",
                new AccountDirectory([])
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Account not found: PLN-9999"
    }

    def "Optional stream flattens present aliases and skips empty ones"() {
        expect:
        directory().aliasesFor(["PLN-1001", "PLN-1002", "PLN-9999"]) ==
                ["anna-main"]
    }

    def "rejects duplicate account numbers"() {
        given:
        def duplicateAccounts = [
                new AccountSummary("PLN-1001", "Anna", Optional.empty()),
                new AccountSummary("PLN-1001", "Another owner", Optional.empty())
        ]

        when:
        new AccountDirectory(duplicateAccounts)

        then:
        thrown(IllegalStateException)
    }

    private static AccountDirectory directory() {
        return new AccountDirectory([
                new AccountSummary(
                        "PLN-1001",
                        "Anna Kowalska",
                        Optional.of("anna-main") // Ta linia tworzy Optional zawierający alias "anna-main" i przekazuje go do AccountSummary dla konta PLN-1001.
                                                       // Dzięki temu testy mogą sprawdzić, że findAlias(...) zwraca alias, a aliasesFor(...) go uwzględnia.
                ),
                new AccountSummary(
                        "PLN-1002",
                        "Jan Nowak",
                        Optional.empty()  // Dla konta bez aliasu użyto Optional.empty().
                )
        ])
    }
}