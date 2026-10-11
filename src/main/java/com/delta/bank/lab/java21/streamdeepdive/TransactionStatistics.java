package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransactionStatistics {

    public long count(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .count();
    }

    public Optional<BigDecimal> minimumAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount) // zbiera wszystkie kwoty z transakcji i tworzy strumień BigDecimal
                .min(BigDecimal::compareTo); // znajduje minimalną kwotę w strumieniu BigDecimal

        /*
        ----------------map

        <R> Stream<R> map(Function<? super T, ? extends R> mapper);

        @FunctionalInterface
        public interface Function<T, R> {
          R apply(T t);
        }

        Function<T, R> function = new Function<T, R>() {
            @Override
            public R apply(T t) {
                return t.amount();
            }
        };

        Function<Transaction, BigDecimal> function = new Function<>() {
          @Override
          public BigDecimal apply(Transaction t) {
            return t.amount();
          }
        };

        lambda expression: Function<Transaction, BigDecimal> function = t -> t.amount();
        typ reference: Function<Transaction, BigDecimal> function = Transaction::amount;



        ----------------min
        Optional<T> min(Comparator<? super T> comparator);

        @FunctionalInterface
        public interface Comparator<T> {
          int compare(T o1, T o2);
        }

        Comparator<BigDecimal> comparator = new Comparator<>() {
          @Override
          public int compare(BigDecimal o1, BigDecimal o2) {
            return o1.compareTo(o2);
          }
        };

        lambda expression: Comparator<BigDecimal> comparator = (o1, o2) -> o1.compareTo(o2);
        typ reference: Comparator<BigDecimal> comparator = BigDecimal::compareTo;
        */

    }

    public Optional<BigDecimal> maximumAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .max(BigDecimal::compareTo);
    }

    public BigDecimal totalAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
               /*
               Jak działa reduce(...):
                BigDecimal.ZERO — wartość startowa, czyli początkowa suma = 0
                BigDecimal::add — sposób łączenia kolejnych elementów, czyli: suma = suma.add(kolejnaKwota)

                To jest odpowiednik mniej więcej takiego kodu:
                   BigDecimal sum = BigDecimal.ZERO;
                   for (Transaction t : transactions) {
                     sum = sum.add(t.amount());
                   }
                   return sum;

                   Jeśli masz np. kwoty: 10.50, 20.00, 5.25, to przebieg wygląda tak:
                     start: 0
                            0 + 10.50 = 10.50
                            10.50 + 20.00 = 30.50
                            30.50 + 5.25 = 35.75
                     Wynik końcowy: 35.75.

                   Dlaczego tutaj nie ma Optional?
                   Bo podajesz wartość początkową (BigDecimal.ZERO),
                   więc nawet dla pustej listy wynik będzie po prostu 0, a nie „brak wyniku”.

                   Czyli:
                      reduce(BigDecimal.ZERO, BigDecimal::add) → zawsze zwróci BigDecimal
                      reduce(BigDecimal::add) → zwróci Optional<BigDecimal>, bo pusty strumień nie ma co sumować
               * */

                 // w reduce jest akumulator.
                 // to funkcja, która mówi reduce, jak połączyć dotychczasowy wynik z kolejnym elementem strumienia.
                .reduce(BigDecimal.ZERO, BigDecimal::add);  // „zwija” cały strumień do jednej wartości.
                // .reduce(BigDecimal::add);  // ale w tym przypadku, jeśli strumień jest pusty, to zwróci Optional.empty().

        /*
T reduce(T identity, BinaryOperator<T> accumulator);

accumulator to funkcja, która mówi reduce, jak połączyć dotychczasowy wynik z kolejnym elementem strumienia.
Dla tego podpisu: T reduce(T identity, BinaryOperator<T> accumulator)

działa to tak:
identity — wartość startowa
accumulator — reguła łączenia: (wynikDoTejPory, kolejnyElement) -> nowyWynik

W tym przykładzie:  .reduce(BigDecimal.ZERO, BigDecimal::add)
accumulator to:
(a, b) -> a.add(b)

czyli:
start od BigDecimal.ZERO
weź pierwszą kwotę i dodaj do wyniku
potem kolejną
potem kolejną itd.

Przykład dla kwot 10, 20, 5:

start = 0
accumulator(0, 10)  -> 10
accumulator(10, 20) -> 30
accumulator(30, 5)  -> 35

        * */
    }

    public Optional<BigDecimal> totalAmountIfPresent(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal::add);  // zwraca Optional<BigDecimal>, bo jeśli strumień jest pusty, to nie ma co sumować.
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}