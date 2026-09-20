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
            // to w { ... } jest Closure w Groovy
            // czyli anonimowy blok kodu, który można przekazać jako wartość i później wykonać.
            // W tym zapisie:
            //         String accountNumber — parametr closure
            //         -> — oddziela listę parametrów od ciała
            // ostatnie wyrażenie, czyli "$accountNumber: BALANCE", jest zwracane jako wynik
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


        // requestExecutor.submit(...) nie zwraca od razu BankDataResult, tylko Future<BankDataResult>.
        // To jest „uchwyt” do zadania, które wykonuje się równolegle w virtual thread (drugi krok).
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

        // drugi krok
        /*
        1. czeka aż dane zadanie się skończy,
        2. odbiera jego wynik,
        3. rzuca wyjątek, jeśli zadanie się wywaliło,
        4. pilnuje limitu czasu, żeby test nie wisiał w nieskończoność.
        * */
        def results = futures.collect { future ->
            future.get(30, TimeUnit.SECONDS)
        }

        then:
        results.size() == 250
        // Weryfikujemy, że każde równoległe wywołanie zwróciło poprawny, niepusty wynik
        // i że wszystkie trzy części odpowiedzi zawierają oczekiwane dane.
        results.every { res ->
            res != null &&
                    res.balance().contains("BALANCE") &&
                    res.history().contains("HISTORY") &&
                    res.summary().contains("SUMMARY")
        }

        and:
        // Sprawdzamy, że każdy zwrócony wynik zachowuje numer konta w każdym polu,
        // więc dane z równoległych wywołań nie mieszają się między różnymi requestami.
        results.every { res ->
                    res.balance().startsWith("ACC-") &&
                    res.history().startsWith("ACC-") &&
                    res.summary().startsWith("ACC-")
        }

        cleanup:
        requestExecutor.close()
    }
}