package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamSafetyExamples {

    // zwraca listę identyfikatorów transakcji typu DEPOSIT
    public List<String> depositIds(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
                .map(Transaction::id)  // returns a Stream<String> of the transaction IDs
                .toList();

        /*
        <R> Stream<R> map(Function<? super T, ? extends R> mapper);

        @FunctionalInterface
        public interface Function<T, R> {
          R apply (T t);
        }

        Function<T, R> function = new Function<T, R>() {
            @Override
            public R apply(T t) {
                return null;
            }

        Function<T, R>
        Function<String, Transaction> function = new Function<String, Transaction>() {
            @Override
            public Transaction apply(String t) {
                // zwraca nowy obiekt Transaction, jeśli długość t jest większa niż 0, w przeciwnym razie zwraca null
                return t.length() > 0 ? new Transaction("id", TransactionType.DEPOSIT, BigDecimal.valueOf(t.length())) : null;
            }

            lambda expression:
            (String t) -> t.length() > 0 ? new Transaction("id", TransactionType.DEPOSIT, BigDecimal.valueOf(t.length())) : null;

         */
    }

    public BigDecimal totalSequential(List<Transaction> transactions) {
        validateTransactions(transactions);


        return transactions.stream()
                .map(Transaction::amount)  // Stream<BigDecimal> of the transaction amounts
                // reduce() is a terminal operation that reduces the elements of a stream to a single value
                .reduce(BigDecimal.ZERO, BigDecimal::add); // reduce the stream to a single BigDecimal value by summing the amounts

    /*
    T reduce(T identity, BinaryOperator<T> accumulator);

    // co to się dzieje w reduce?
    // T reduce(BigDecimal identity, BinaryOperator<BigDecimal> accumulator);
    // reduce(BigDecimal.ZERO, BigDecimal::add) bierze wszystkie elementy strumienia i składa je do jednej wartości.
    //    mamy to akumulator
    // W tym przypadku:
    //   BigDecimal.ZERO to wartość początkowa (identity)
    //   BigDecimal::add to sposób łączenia kolejnych elementów (accumulator)
    // Czyli dla kwot 10, 20, 5 dzieje się:
    //  start: 0
    //  0 + 10 = 10
    //  10 + 20 = 30
    //  30 + 5 = 35
    * */

    }


    public BigDecimal totalParallel(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.parallelStream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<String> idsInEncounterOrder(List<Transaction> transactions) {
        validateTransactions(transactions);


        // parallelStream() może przetwarzać elementy równolegle,
        // ale toList() zachowuje encounter order, więc wynikowa lista ID
        // będzie miała taką samą kolejność jak wejściowa lista transactions.
        // To jest ważne, bo mimo użycia strumienia równoległego wynik nie jest „losowo” uporządkowany 
        //    — toList() dla uporządkowanego źródła zachowuje kolejność elementów.
        return transactions.parallelStream()
                .map(Transaction::id) // zwraca Stream<String> identyfikatorów transakcji
                .toList();
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}