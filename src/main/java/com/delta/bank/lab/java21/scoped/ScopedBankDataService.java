package com.delta.bank.lab.java21.scoped;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

public class ScopedBankDataService {

    private final BankDataLoader loader;

    public ScopedBankDataService(BankDataLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
    }

    public BankDataResult loadAll(
            String accountNumber,
            RequestContext context
    ) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(context, "context must not be null");

        // Ustawia RequestContext tylko na czas wykonania operacji ładowania,
        // dzięki czemu wszystkie wywołania wewnątrz loadWithinContext(accountNumber)
        // mają dostęp do bieżącego kontekstu żądania.
        // czyli:
        // Ten fragment opakowuje wywołanie lambda: loadWithinContext(accountNumber) w aktywny RequestContext,
        // żeby dalsze operacje — także uruchamiane niżej współbieżnie — mogły korzystać z danych kontekstowych
        // powiązanych z bieżącym żądaniem.
        return RequestContextHolder.withContext(
                context,
                () -> loadWithinContext(accountNumber)
        );
    }

    private BankDataResult loadWithinContext(String accountNumber) {

        // Tworzy zakres współbieżnych zadań, który przerwie pozostałe operacje,
        // jeśli którekolwiek z nich zakończy się błędem.
        var scope = new StructuredTaskScope.ShutdownOnFailure();

        // Uruchamia trzy niezależne operacje równolegle w ramach StructuredTaskScope.
        // `join()` czeka na zakończenie wszystkich zadań, a `throwIfFailed()` przerywa
        // dalsze przetwarzanie, jeśli którekolwiek zakończyło się błędem.
        // Gdy wszystko powiedzie się poprawnie, wyniki są pobierane i składane
        // do jednego obiektu `BankDataResult`.
        try (scope) {
            var balance = scope.fork(() -> loader.loadBalance(accountNumber));

            /*
            public <U extends T> Subtask<U> fork(Callable<? extends U> task) {
                Objects.requireNonNull(task, "task must not be null");
                Subtask<U> subtask = new Subtask<>(task);
                submit(subtask);
                return subtask;


                @FunctionalInterface
                public interface Callable<V> {
                   V call() throws Exception;
                }

                var balance = scope.fork( new java.util.concurrent.Callable<>() {
                  @Override
                  public String call() throws Exception {
                    return loader.loadBalance(accountNumber);
                  }
                });


                  public String call() throws Exception {
                    return loader.loadBalance(accountNumber);
                  }

                  zapis  w formie lambda:
                  () -> loader.loadBalance(accountNumber)


                var balance = scope.fork(() -> loader.loadBalance(accountNumber))  // to jest lambda, która implementuje Callable<String>

             */

            var history = scope.fork(() -> loader.loadHistory(accountNumber));
            var summary = scope.fork(() -> loader.loadSummary(accountNumber));

            scope.join();
            scope.throwIfFailed();

            return new BankDataResult(
                    balance.get(),
                    history.get(),
                    summary.get()
            );

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