package com.delta.bank.lab.java21.virtualthreads;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VirtualThreadBankService {

    private final BankDataLoader loader;

    public VirtualThreadBankService(BankDataLoader loader) {
        this.loader = loader;
    }

    public BankDataResult loadAll(String accountNumber) {

        /*
         * Ten fragment łączy kilka mechanizmów Java 21:
         *
         * 1. LAMBDA
         *
         *    Wyrażenie:
         *
         *        () -> loader.loadAccountBalance(accountNumber)
         *
         *    jest krótkim zapisem zadania typu Callable<String>.
         *    Lambda nie przyjmuje argumentów i zwraca String.
         *
         *    Można ją zapisać bardziej rozwlekle:
         *
         *        Callable<String> task = new Callable<>() {
         *            @Override
         *            public String call() {
         *                return loader.loadAccountBalance(accountNumber);
         *            }
         *        };
         *
         * 2. CAPTURING LAMBDA, CZYLI PRZECHWYTYWANIE ZMIENNYCH
         *
         *    Lambda korzysta z wartości znajdujących się poza jej ciałem:
         *
         *    - accountNumber — parametr metody loadAll(...),
         *    - loader — pole bieżącego obiektu; technicznie lambda korzysta
         *      z referencji this.
         *
         *    Jest to zachowanie podobne do closure znanego z Groovy.
         *    W Javie precyzyjniej mówimy: capturing lambda.
         *
         *    Lokalne zmienne przechwytywane przez lambdę muszą być final albo
         *    effectively final, czyli po przypisaniu nie mogą być zmieniane.
         *
         * 3. EXECUTOR VIRTUAL THREADS
         *
         *    executor.submit(...) przyjmuje lambdę jako zadanie Callable<String>.
         *    Każde wywołanie submit(...) przekazuje osobne zadanie do wykonania.
         *
         *    W tym przykładzie executor tworzy dla zadań virtual threads:
         *
         *        Future<String> balance = executor.submit(...);
         *
         *    Metoda submit(...) nie zwraca od razu Stringa. Zwraca Future<String>,
         *    czyli uchwyt do wyniku, który będzie dostępny po zakończeniu zadania.
         *
         *    Wartość pobieramy później przez:
         *
         *        future.get();
         *
         * 4. RÓWNOLEGŁOŚĆ
         *
         *    Trzy zadania są zgłaszane do executora przed pobraniem ich wyników:
         *
         *    - pobranie salda,
         *    - pobranie historii,
         *    - pobranie podsumowania.
         *
         *    Dzięki temu mogą wykonywać się niezależnie i równolegle.
         *    Samo użycie virtual threads nie oznacza jednak automatycznie,
         *    że operacje CPU będą szybsze. Ten mechanizm jest szczególnie przydatny
         *    przy zadaniach oczekujących na I/O, np. bazę danych lub HTTP.
         *
         * 5. TRY-WITH-RESOURCES
         *
         *    ExecutorService jest zasobem, który należy zamknąć po zakończeniu pracy.
         *    Konstrukcja try (...) gwarantuje jego zamknięcie również wtedy,
         *    gdy jedno z zadań zakończy się wyjątkiem.
         *
         *    W Java 21 ExecutorService implementuje AutoCloseable, dlatego można
         *    używać go w try-with-resources.
         *
         * 6. OBSŁUGA WYNIKÓW I WYJĄTKÓW
         *
         *    getResult(...) wywołuje Future.get():
         *
         *    - zwraca wynik zadania,
         *    - obsługuje InterruptedException,
         *    - obsługuje ExecutionException,
         *    - przywraca flagę przerwania wątku przez Thread.currentThread().interrupt().
         *
         * Podsumowanie:
         *
         *    lambda
         *        -> przechwytuje accountNumber oraz this.loader
         *        -> jest przekazana do executor.submit(...)
         *        -> zostaje wykonana jako zadanie na virtual thread
         *        -> zwraca wynik dostępny przez Future<String>
         *
         * To można konceptualnie nazwać closure-like behavior, ale precyzyjny termin
         * używany w Javie to capturing lambda.
         */
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try (executor) {

            /*
             * Zgłasza zadanie pobrania salda do executora virtual threads.
             *
             * Lambda jest Callable<String>, które przechwytuje accountNumber
             * oraz referencję this.loader.submit(...) zwraca Future<String>,
             * czyli uchwyt do wyniku dostępnego później przez balance.get().
             */
            Future<String> balance = executor.submit(
                    // Nie wykonuje się od razu w tym miejscu
                    // Ona jest przekazywana do: executor.submit(...)
                    // wykonuje się wirtualnym wątkiem, który pobiera historię transakcji
                    // dopiero fizycznie wykona się to, kiedy wirtualny wątek zostanie uruchomiony przez executor
                    // a to może nastąpić w dowolnym momencie po submit, w zależności od dostępności wirtualnych wątków i harmonogramu executor
                    () -> loader.loadAccountBalance(accountNumber)
            );

            Future<String> history = executor.submit(
                    () -> loader.loadTransactionHistory(accountNumber)
            );

            Future<String> summary = executor.submit(
                    () -> loader.loadAccountSummary(accountNumber)
            );

            return new BankDataResult(
                    getResult(balance),
                    getResult(history),
                    getResult(summary)
            );
        }
    }

    // Pobiera wynik zadania asynchronicznego i zamienia wyjątki checked na IllegalStateException,
    // zachowując status przerwania wątku
    private String getResult(Future<String> future) {
        try {
 /*
  zwróci wartość zwróconą wcześniej przez zadanie przekazane do executor.submit(...), np.:
   - dla balance → wynik loader.loadAccountBalance(accountNumber):     zwróci zamokowane "PLN-1001: BALANCE: 1000.00 PLN"
   - dla history → wynik loader.loadTransactionHistory(accountNumber): zwróci zamokowane "PLN-1001: TRANSACTION_HISTORY: [TEST_TX_1, TEST_TX_2, TEST_TX_3]"
   - dla summary → wynik loader.loadAccountSummary(accountNumber):     zwróci zamokowane "PLN-1001: SUMMARY: +150.00 PLN"
 */
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Bank data loading was interrupted",
                    exception
            );
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "Bank data loading failed",
                    exception.getCause()
            );
        }
    }
}