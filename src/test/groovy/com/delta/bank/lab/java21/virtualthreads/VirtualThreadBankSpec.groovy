package com.delta.bank

import com.delta.bank.lab.java21.virtualthreads.BankDataLoader
import com.delta.bank.lab.java21.virtualthreads.BankDataResult
import com.delta.bank.lab.java21.virtualthreads.VirtualThreadBankService
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VirtualThreadBankSpec extends Specification {

    def "loads balance history and summary using virtual threads"() {
        given:
        def loader = Mock(BankDataLoader)
        def service = new VirtualThreadBankService(loader)

        and:
        // Oczekujemy jednego wywołania loadera dla salda konta i zwracamy przykładową odpowiedź
        // W tym teście „rzeczywista metoda” loader.loadAccountBalance(...) w ogóle się nie wykona.
        // loader jest Mock(BankDataLoader), więc Spock przechwytuje wywołanie i zwraca dokładnie to, co podałeś po >>.
        1 * loader.loadAccountBalance("PLN-1001") >> "PLN-1001: BALANCE: 1000.00 PLN"
        1 * loader.loadTransactionHistory("PLN-1001") >> "PLN-1001: TRANSACTION_HISTORY: [TEST_TX_1, TEST_TX_2, TEST_TX_3]"
        1 * loader.loadAccountSummary("PLN-1001") >> "PLN-1001: SUMMARY: +150.00 PLN"

        when:
        def result = service.loadAll("PLN-1001")

        then:
        result instanceof BankDataResult
        result.balance() == "PLN-1001: BALANCE: 1000.00 PLN"
        result.history() == "PLN-1001: TRANSACTION_HISTORY: [TEST_TX_1, TEST_TX_2, TEST_TX_3]"
        result.summary() == "PLN-1001: SUMMARY: +150.00 PLN"
    }

    def "loads data for another account"() {
        given:
        def loader = Mock(BankDataLoader)
        def service = new VirtualThreadBankService(loader)

        and:
        1 * loader.loadAccountBalance("EUR-2001") >> "BALANCE: 500.00 EUR"
        1 * loader.loadTransactionHistory("EUR-2001") >> "HISTORY: 5 transactions"
        1 * loader.loadAccountSummary("EUR-2001") >> "SUMMARY: +75.00 EUR"

        when:
        def result = service.loadAll("EUR-2001")

        then:
        result.balance() == "BALANCE: 500.00 EUR"
        result.history() == "HISTORY: 5 transactions"
        result.summary() == "SUMMARY: +75.00 EUR"
    }

    def "propagates exception from bank data loader"() {
        given:
        def loader = Mock(BankDataLoader)
        def service = new VirtualThreadBankService(loader)

        and:
        1 * loader.loadAccountBalance("PLN-1001") >>
                { throw new IllegalStateException("Database unavailable") }

        when:
        service.loadAll("PLN-1001")

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Bank data loading failed"
        exception.cause.message == "Database unavailable"
    }

    def "returns immutable result record"() {
        given:
        def result = new BankDataResult(
                "BALANCE",
                "HISTORY",
                "SUMMARY"
        )

        expect:
        result.balance() == "BALANCE"
        result.history() == "HISTORY"
        result.summary() == "SUMMARY"
    }

/*
Co ten test rzeczywiście sprawdza?
Test uruchamia:
 - 250 równoległych wywołań service.loadAll(...)
 - Każde loadAll(...) uruchamia wewnętrznie trzy zadania:
   1. pobranie salda
   2. pobranie historii
   3. pobranie podsumowania

W przybliżeniu test tworzy więc:
 - 250 zewnętrznych virtual threads
   +
   750 wewnętrznych virtual threads
   =
   około 1000 zadań
Każde zadanie loadera czeka 20 ms, dzięki czemu test rzeczywiście sprawdza obsługę wielu
równoległych operacji oczekujących.
*/
    def "handles 250 concurrent requests"() {
        given:
        // Używamy Stub zamiast Mock, bo w tym teście nie sprawdzamy interakcji,
        // tylko symulujemy przewidywalne odpowiedzi loadera pod dużym obciążeniem współbieżnym.
        def loader = Stub(BankDataLoader) {
            // Symuluje wolne pobranie salda (np. z bazy lub API),
            // aby przetestować zachowanie przy współbieżnych wywołaniach.
            loadAccountBalance(_) >> { String accountNumber ->
                Thread.sleep(20)
                "$accountNumber: BALANCE"
            }

            loadTransactionHistory(_) >> { String accountNumber ->
                Thread.sleep(20)
                "$accountNumber: HISTORY"
            }

            loadAccountSummary(_) >> { String accountNumber ->
                Thread.sleep(20)
                "$accountNumber: SUMMARY"
            }
        }

        def service = new VirtualThreadBankService(loader)
        def requestExecutor = Executors.newVirtualThreadPerTaskExecutor()

        when:
        def futures = (1..250).collect { index ->
            requestExecutor.submit({ ->
                service.loadAll("ACC-$index")
            } as Callable<BankDataResult>)
        }
        /*  lub prościej
        def futures = (1..250).collect { index ->
            Callable<BankDataResult> task = new Callable<BankDataResult>() {
                @Override
                BankDataResult call() {
                    return service.loadAll("ACC-$index")
                }
            }
            requestExecutor.submit(task)
        }*/

        def results = futures.collect { future ->
            future.get(30, TimeUnit.SECONDS)
        }

        then:
        results.size() == 250
        results.every { result ->
            result != null &&
                    result.balance().contains("BALANCE") &&
                    result.history().contains("HISTORY") &&
                    result.summary().contains("SUMMARY")
        }

        and:
        results.every { result ->
            result.balance().startsWith("ACC-") &&
                    result.history().startsWith("ACC-") &&
                    result.summary().startsWith("ACC-")
        }

        cleanup:
        requestExecutor.close()
    }
}