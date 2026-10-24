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

    public <T> T execute(Callable<T> operation) throws Exception {

        /*
        Callable<String> callable = (Callable<String>) operation;
        Callable<String> callableLambda = () -> "statement loaded";

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
            return "statement loaded";  ------> to z testu BankCallLimiterSpec
        };

        lambda :
        Callable<String> callablelambda = () -> "statement loaded";  ------> to z testu BankCallLimiterSpec
        * */


        Objects.requireNonNull(operation, "operation must not be null");

        semaphore.acquire();
        try {
            /*
             * Wywołuje przekazaną operację i zwraca jej wynik.
             * Zezwolenie semafora jest zwalniane w bloku finally, także gdy operacja zgłosi wyjątek.
             */
            return operation.call();
        } finally {
            semaphore.release();
        }
    }
}