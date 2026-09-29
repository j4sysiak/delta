package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.domain.*
import spock.lang.Specification

class SpockMocksSpec extends Specification {

    def "finds account using repository mock"() {
        given:
        def repository = Mock(BankAccountRepository)
        def transactionRepository = Mock(AccountTransactionRepository)
        def service = new BankAccountService(repository, transactionRepository)

        def account = new BankAccountEntity(
                "PLN-MOCK-01",
                "Alice",
                new BigDecimal("1000.00"),
                "PLN"
        )

        when:
        def result = service.find("PLN-MOCK-01")

        then:
        // Service powinien raz odczytać konto z repozytorium
        // i dostać `account` jako wynik (lub Optional.of(account))
        // Optional.of(account) oznacza że konto istnieje w repozytorium i jest zwracane jako wynik
        // w przeciwnym razie, gdyby konto nie istniało, repozytorium zwróciłoby Optional.empty()
        // W tym przypadku, ponieważ konto istnieje, repozytorium zwraca Optional.of(account) bo to chcemy przetestować
        // gdybyś chciał przetestować przypadek gdy konto nie istnieje, to repozytorium zwróciłoby Optional.empty() i wtedy wynik byłby null lub wyjątek w zależności od implementacji serwisu
        1 * repository.findById("PLN-MOCK-01") >> Optional.of(account)

        result.is(account)
        result.getNumber() == "PLN-MOCK-01"
        result.getOwner() == "Alice"
        result.getBalance() == new BigDecimal("1000.00")
        result.getCurrency() == "PLN"
    } 

    def "deposits money and saves account and transaction"() {
        given:
        def repository = Mock(BankAccountRepository)
        def transactionRepository = Mock(AccountTransactionRepository)
        def service = new BankAccountService(repository, transactionRepository)

        def account = new BankAccountEntity(
                "PLN-MOCK-02",
                "Alice",
                new BigDecimal("1000.00"),
                "PLN"
        )

        when:
        def result = service.deposit(
                "mock-deposit-01",
                "PLN-MOCK-02",
                new BigDecimal("250.00")
        )

        then:
        1 * repository.findById("PLN-MOCK-02") >> Optional.of(account)

        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "mock-deposit-01",
                "PLN-MOCK-02",
                TransactionType.DEPOSIT
        ) >> Optional.empty()

        1 * repository.save(account) >> account

        1 * transactionRepository.save({
            AccountTransactionEntity transaction ->
                transaction.getAccountNumber() == "PLN-MOCK-02" &&
                        transaction.getType() == TransactionType.DEPOSIT &&
                        transaction.getAmount() == new BigDecimal("250.00") &&
                        transaction.getCurrency() == "PLN" &&
                        transaction.getTransferRequestId() == "mock-deposit-01"
        })

        result.is(account)
        result.getBalance() == new BigDecimal("1250.00")
    }

    def "does not save duplicate deposit"() {
        given:
        def repository = Mock(BankAccountRepository)
        def transactionRepository = Mock(AccountTransactionRepository)
        def service = new BankAccountService(repository, transactionRepository)

        def account = new BankAccountEntity(
                "PLN-MOCK-03",
                "Alice",
                new BigDecimal("1000.00"),
                "PLN"
        )

        def existingTransaction = Stub(AccountTransactionEntity)

        when:
        def result = service.deposit(
                "mock-deposit-duplicate",
                "PLN-MOCK-03",
                new BigDecimal("250.00")
        )

        then:
        1 * repository.findById("PLN-MOCK-03") >> Optional.of(account)

        1 * transactionRepository.findFirstByTransferRequestIdAndAccountNumberAndType(
                "mock-deposit-duplicate",
                "PLN-MOCK-03",
                TransactionType.DEPOSIT
        ) >> Optional.of(existingTransaction)

        0 * repository.save(_)
        0 * transactionRepository.save(_)

        result.is(account)
        result.getBalance() == new BigDecimal("1000.00")
    }

    def "propagates repository failure while finding account"() {
        given:
        def repository = Mock(BankAccountRepository)
        def transactionRepository = Mock(AccountTransactionRepository)
        def service = new BankAccountService(repository, transactionRepository)

        when:
        service.find("PLN-MOCK-04")

        then:
        1 * repository.findById("PLN-MOCK-04") >>
                { throw new IllegalStateException("Database unavailable") }

        def exception = thrown(IllegalStateException)
        exception.message == "Database unavailable"
    }
}