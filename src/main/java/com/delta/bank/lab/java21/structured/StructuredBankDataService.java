package com.delta.bank.lab.java21.structured;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

public class StructuredBankDataService {

    private final BankDataLoader loader;

    public StructuredBankDataService(BankDataLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
    }

    public BankDataResult loadAll(String accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");

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