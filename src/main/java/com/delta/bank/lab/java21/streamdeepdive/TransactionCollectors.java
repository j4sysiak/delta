package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransactionCollectors {

    public Map<String, Transaction> byId(List<Transaction> transactions) {
        validateTransactions(transactions);
/*
        Function<Transaction, String> id = Transaction::id;
        Function<Transaction, Transaction> identity = Function.identity();

        static <T> Function<T, T> identity() {
            return t -> t;
        }
        lambda expression t -> t    is a function that takes an argument t and returns it unchanged.
                                    It is a simple identity function that can be used in various contexts where a function is required, but no transformation is needed.

        Collector<Transaction, ?, Map<String, Transaction>> map = Collectors.toMap(
                id,
                identity
        );

        return transactions.stream()
                .collect(map);
*/

        // To po prostu zamienia List<Transaction> na Map<String, Transaction>, gdzie:
        // kluczem jest Transaction::id
        // wartością jest cały obiekt Transaction bez zmian
        return transactions.stream()
                .collect(Collectors.toMap(
                        /*
                           z każdego Transaction weź id jako klucz
                           jako wartość włóż cały ten sam obiekt.
                           Czyli z tego:
                             [
                                tx1(id="A1"),
                                tx2(id="A2")
                             ]
                             zrób mapę toMap():
                             {
                                "A1": tx1(id="A1"),
                                "A2": tx2(id="A2")
                             }

                         */

                        Transaction::id,  // transaction -> transaction.id(),
                        //transaction -> transaction   czyli: „weź element i zwróć go takim, jaki jest”.
                        Function.identity()  // cały obiekt Transaction bez zmian: t -> t
                ));

        /*
        Function.identity() to po prostu gotowa wersja x -> x.
         Czyli:
           dostaje obiekt Transaction
            zwraca dokładnie ten sam obiekt
           Więc dla listy np.:
           [
            Transaction("t1", ...),
            Transaction("t2", ...)
           ]
           wynik będzie mniej więcej taki:
              {
                "t1": Transaction("t1", ...),
                "t2": Transaction("t2", ...)
              }

           To jest wygodne, gdy chcesz szybko znaleźć transakcję po id.
           Uwaga: jeśli dwa elementy mają to samo id, Collectors.toMap(...) rzuci wyjątek.
        * */
    }

    public Map<String, BigDecimal> totalsByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);


        /*
        Efekt końcowy: powstaje Map<String, BigDecimal>, gdzie:
          klucz = numer konta,
          wartość = suma wszystkich kwot dla tego konta.
        * */
        return transactions.stream()
                .collect(Collectors.toMap(
                        Transaction::accountNumber,  // Bierze Transaction i zwraca numer konta — to jest klucz mapy.
                        Transaction::amount,         // Bierze Transaction i zwraca kwotę — to jest wartość mapy.
                        BigDecimal::add   // To jest merge function: gdy dwa różne Transaction mają ten sam accountNumber, to zamiast wyjątku sumujesz kwoty.
                ));

        /*
        return transactions.stream()
        .collect(Collectors.toMap(
                transaction -> transaction.accountNumber(),
                transaction -> transaction.amount(),
                (a, b) -> a.add(b)  //  w tym miejscu jest funkcją scalającą (merge function), przekazywaną do Collectors.toMap(...).toMap(...).
                                    // (existingAmount, newAmount) -> existingAmount.add(newAmount)
        ));
        * */
    }

    public Map<String, List<Transaction>> byAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber
                ));

        /*
        @FunctionalInterface
        public interface Function<T, R> {
          R apply(T t);
        }

        T = Transaction
        R = String
        Function<T, R> function = new Function<T, R>() {
            @Override
            public R apply(T t) {
                return t.getAccountNumber();  // implementacja metody apply
            }
        };

        lambda expression equivalent:
        Function<Transaction, String> function = transaction -> transaction.getAccountNumber();

        reference method equivalent:
        Function<Transaction, String> function = Transaction::getAccountNumber;
        * */
    }

    public Map<Boolean, List<Transaction>> depositsAndOthers(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.partitioningBy(
                        transaction -> transaction.type() == TransactionType.DEPOSIT
                ));
    }

    public Map<String, List<BigDecimal>> amountsByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,
                        Collectors.mapping(
                                Transaction::amount,
                                Collectors.toList()
                        )
                ));
    }

    public Map<String, TransactionGroupSummary> summariesByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.mapping(
                                        Transaction::amount,
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )
                                ),
                                TransactionGroupSummary::new
                        )
                ));
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}