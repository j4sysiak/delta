package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamBasics {

    public List<BigDecimal> depositAmounts(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }

        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
                 // dla każdej transakcji pobiera jej kwotę i zwraca Stream<BigDecimal>
                .map(Transaction::amount)   // .map(transaction -> transaction.amount())
                .toList();
    }

    /*
    1.
    Stream<T> filter(Predicate<? super T> predicate);

    @FunctionalInterface
    public interface Predicate<T> {
      boolean test(T t);
    }

    Predicate predicate = new Predicate() {
        @Override
        public boolean test(Object o) {
            return false;
        }
    };

    Predicate predicate = new Predicate<Transaction>() {
    @Override
    public boolean test(Transaction transaction) {
        return transaction.type() == TransactionType.DEPOSIT;
    }
}

   wersja lambda:   Predicate predicate = transaction -> transaction.type() == TransactionType.DEPOSIT;

    wersja lambda w filtrze juz na gotowo: .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)



    2.
    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

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

    Function<T, R> function = new Function<Transaction, BigDecimal>() {
        @Override
        public BigDecimal apply(Transaction transaction) {
            return transaction.amount();
        }
    };

    wersja lambda: Function<Transaction, BigDecimal> function = transaction -> transaction.amount();

    wersja lambda w filtrze juz na gotowo:  .map(transaction -> transaction.amount())
    wersja lambda w filtrze juz na gotowo:  .map(Transaction::amount)  // referencja do metody
    */
}