package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamTransformations {

    public List<Transaction> filterByType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .toList();
    }

    public List<BigDecimal> amountsForType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .map(Transaction::amount)
                .toList();
    }

    public List<Transaction> flattenHistories(
            List<List<Transaction>> histories
    ) {
        Objects.requireNonNull(histories, "histories must not be null");

        for (var history : histories) {
            Objects.requireNonNull(history, "history must not be null");
            validateTransactions(history);
        }

        return histories.stream()
                // .flaMap(Transactions -> transactions.stream())
                .flatMap(List::stream)
                .toList();

        /*
        <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

        @FunctionalInterface
        public interface Function<T, R> {
           R apply(T t);
        }

        Function<T, R> function = new Function<T, R>() {
            @Override
            public R apply(T t) {
                return null;
            }
        };

        Function<List<Transaction>, Stream<Transaction>> function = new Function<List<Transaction>, Stream<Transaction>>() {
            @Override
            public Stream<Transaction> apply(List<Transaction> transactions) {
                return transactions.stream();
            }
        };

        lambda version:  Function<<List<Transaction>, Stream<Transaction>> function = transactions -> transactions.stream();
        typ reference version: .flatMap(List::stream)

        * */
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}