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
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var balance = scope.fork(() -> loader.loadBalance(accountNumber));
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