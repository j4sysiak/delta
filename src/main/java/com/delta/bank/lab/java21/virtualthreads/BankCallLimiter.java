package com.delta.bank.lab.java21.virtualthreads;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class BankCallLimiter {

    private final Semaphore semaphore;

    /**
     * Tworzy limiter ograniczający liczbę równoczesnych wywołań do podanej wartości.
     *
     * @param maxConcurrentCalls maksymalna liczba wywołań wykonywanych jednocześnie; musi być dodatnia
     * @throws IllegalArgumentException gdy {@code maxConcurrentCalls} jest mniejsze lub równe zero
     */
    public BankCallLimiter(int maxConcurrentCalls) {
        if (maxConcurrentCalls <= 0) {
            throw new IllegalArgumentException(
                    "maxConcurrentCalls must be positive"
            );
        }
        // Creates a Semaphore with the given number of permits
        this.semaphore = new Semaphore(maxConcurrentCalls);
    }

    // T bedzie w pierwszym teście String, w drugim Integer, w trzecim Double, w czwartym DepositEvent
    public <T> T execute(Callable<T> operation) throws Exception {
        Callable<String> callable = (Callable<String>) operation;
        Callable<String> callableLambda = () -> "statement loaded";

        /*

        @FunctionalInterface
        public interface Callable<V> {
          V call() throws Exception;
        }

        Callable<V> callable = new Callable<V>() {
            @Override
            public V call() throws Exception {
                // Your code here
                return result;
            }
        };

        Callable<String> callable = () -> {
            // Your code here
            return "statement loaded";
        };

        lambda :
        Callable<String> callablelambda = () -> "statement loaded";
        * */


        Objects.requireNonNull(operation, "operation must not be null");

        semaphore.acquire();
        try {
            /*
             wywołanie callableLambda.call() zwróci warość parametru wejściowego tej głównej metody:  "statement loaded".
             Sama linia tylko tworzy Callable; nie wykonuje go.
             W execute() zwracany jest wynik operation.call(), a nie callableLambda, bo ta zmienna nie jest tam używana.
            * */
            return operation.call();
        } finally {
            semaphore.release();
        }
    }
}