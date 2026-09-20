package com.delta.bank.lab.java21.concurrency;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Objects;

/*
    wywołuje wiele równoległych zadań
    każde zadanie wykonuje deposit(...)

Ten runner:
1. tworzy executor virtual threads,
2. uruchamia wiele wpłat,
3. zapamiętuje Future każdego zadania,
4. czeka na zakończenie wszystkich zadań,
5. odczytuje saldo końcowe,
6. zamyka executor przez try-with-resources.
**/

public class VirtualThreadDepositRunner {

    public BigDecimal runDeposits(
            DepositAccount account,
            int depositCount,
            BigDecimal amount
    ) {
        Objects.requireNonNull(account, "account must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (depositCount <= 0) {
            throw new IllegalArgumentException("depositCount must be greater than zero");
        }

        List<Future<?>> futures = new ArrayList<>(depositCount);

        // 1. tworzy executor virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < depositCount; index++) {
                // 2. uruchamia wiele wpłat
                futures.add(executor.submit(() -> account.deposit(amount)));
            }
            // 4. czeka na zakończenie wszystkich zadań
            waitForAll(futures);

            // 5. odczytuje saldo końcowe
            return account.balance();

            // 6. zamyka executor przez try-with-resources
        }
    }

    private void waitForAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Deposit processing was interrupted",
                        exception
                );
            } catch (ExecutionException exception) {
                throw new IllegalStateException(
                        "Deposit processing failed",
                        exception.getCause()
                );
            }
        }
    }
}