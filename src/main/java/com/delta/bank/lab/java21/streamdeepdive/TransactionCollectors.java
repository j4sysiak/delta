package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
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

    /*
    Na wyjściu powstaje Map<String, List<BigDecimal>>, gdzie:
      klucz = numer konta,
      wartość = lista wszystkich kwot dla tego konta.
    Czyli nie dostajesz listy całych transakcji, tylko listy kwot pogrupowane po koncie.
    * */
    public Map<String, List<BigDecimal>> amountsByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        // czyli: pogrupuj transakcje po numerze konta i z każdej zostaw tylko amount.
        // przykład: dla listy transakcji:
        // [
        //   Transaction(accountNumber="A1", amount=100),
        //   Transaction(accountNumber="A1", amount=200),
        //   Transaction(accountNumber="A2", amount=300)
        // ]
        // wynik będzie:
        // {
        //   "A1": [100, 200],
        //   "A2": [300]
        // }
        Collector<Transaction, ?, List<BigDecimal>> mapping = Collectors.mapping(
                Transaction::amount,  // 100, 200, 300
                Collectors.toList()  // [100, 200, 300]  dla każdego konta
        );
        // groupingBy(Transaction::accountNumber) samo w sobie grupuje całe obiekty Transaction, więc wynik to:
        // groupingBy(...) → zachowuje całe Transaction
        // wynik to: Map<String, List<Transaction>>
        // czyli np. dla konta "A1" dostajesz listę wszystkich transakcji dla tego konta.
        //   {
        //     "A1": [tx1, tx2],
        //     "A2": [tx3]
        //   }


        // groupingBy(..., mapping(...)) → po grupowaniu robi transformację elementów w grupie
        // czyli majac juz pogrupowane w pierwszej części po accountNumber, grupuje teraz przez mapping(...)
        // z każdej transakcji bierze tylko amount, więc wynik to:
        // {
        //   "A1": [100, 200],
        //   "A2": [300]
        // }
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,  // lambda expression: transaction -> transaction.accountNumber()
                        mapping  // as Collector<Transaction, ?, List<BigDecimal>>
                ));

        /*
        Collector<T, ?, Map<K, D>> groupingBy(Function<? super T, ? extends K> classifier,
                                          Collector<? super T, A, D> downstream) {
            return groupingBy(classifier, HashMap::new, downstream);
        }
        * */

    }

    public Map<String, TransactionGroupSummary> summariesByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,
                        /*
                          To teeing(...) liczy dwie rzeczy naraz dla każdej grupy transakcji i skleja je w jeden obiekt TransactionGroupSummary.
                          W tym fragmencie, po wcześniejszym groupingBy(Transaction::accountNumber, ...), dla każdego konta dzieje się to:

                          1. Collectors.counting()
                             liczy, ile transakcji jest na tym koncie
                             wynik: Long
                          2. Collectors.mapping(Transaction::amount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                             najpierw z każdej transakcji bierze tylko amount,
                             a potem sumuje wszystkie kwoty od BigDecimal.ZERO
                             wynik: BigDecimal
                          3. TransactionGroupSummary::new
                             dostaje te dwa wyniki i tworzy obiekt: new TransactionGroupSummary(count, total)

                          Efekt końcowy: dla każdego accountNumber dostajesz podsumowanie typu: Map<String, TransactionGroupSummary>
                          {
                            "A1": TransactionGroupSummary(count=2, total=300),
                            "A2": TransactionGroupSummary(count=1, total=300)
                          }
                          czyli:
                             2 = liczba transakcji konta A1
                             300.00 = suma ich kwot.
                        */
                        Collectors.teeing(
                                Collectors.counting(),  // par.1 Collector<? super T, ?, R1> downstream1
                                Collectors.mapping(
                                        Transaction::amount,
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )  //par.2  Collector<? super T, ?, R2> downstream2
                                ),
                                TransactionGroupSummary::new  //par.3 Bifunction<Long, BigDecimal, TransactionGroupSummary>  (count, total) -> new TransactionGroupSummary(count, total)
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