package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransactionQueries {

    public List<Transaction> sortedByAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .sorted(Comparator
                        .comparing(Transaction::amount)   //  reference:  Function<Transaction, BigDecimal> function = Transaction::amount;
                        .thenComparing(Transaction::id)   //  reference:  Function<Transaction, BigDecimal> function = Transaction::id;
                )
                .toList();

/*
To po prostu sortowanie listy transactions po dwóch kluczach.
Działa to tak:
  1. transactions.stream() — zamienia listę na strumień
  2. sorted(...) — każe posortować elementy
  3. Comparator.comparing(Transaction::amount) — najpierw porównuje transakcje po amount()
  4. .thenComparing(Transaction::id) — jeśli dwie transakcje mają tę samą kwotę, to dogrywa kolejność po id()
  5. .toList() — zbiera wynik z powrotem do listy
      Transaction::amount to skrót od lambdy:  transaction -> transaction.amount()
      Transaction::id to skrót od lambdy:  transaction -> transaction.id()


      Czyli cały ten comparator można zapisać bardziej „łopatologicznie” tak:

      return transactions.stream()
        .sorted((t1, t2) -> {
            int byAmount = t1.amount().compareTo(t2.amount());
            if (byAmount != 0) {
                return byAmount;
            }
            return t1.id().compareTo(t2.id());
        })
        .toList();


--------------------- sorted

        Stream<T> sorted(Comparator<? super T> comparator);

        @FunctionalInterface
        public interface Comparator<T> {
          int compare(T o1, T o2);
        }

        Comparator<T> comparator = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return 0;
            }
        };

        Comparator<T> comparator = new Comparator<Transaction>() {
            @Override
            public int compare(Transaction o1, Transaction o2) {
                return o1.amount().compareTo(o2.amount());
            }

        lambda:    Comparator<Transaction> comparator = (o1, o2) -> o1.amount().compareTo(o2.amount());
        };

----------------------   comparing()

        public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            Function<? super T, ? extends U> keyExtractor) {
                Objects.requireNonNull(keyExtractor);
                return (Comparator<T> & Serializable) (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
            }

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

            lambda:   Function<Transaction, BigDecimal> function = transaction -> transaction.amount();
            reference:  Function<Transaction, BigDecimal> function = Transaction::amount;

*/
    }

    public List<BigDecimal> distinctAmounts(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                 // usuwa duplikaty ze strumienia, zostawiając tylko pierwsze wystąpienie każdej wartości.
                 // distinct() sprawia, że w wyniku każda kwota pojawi się tylko raz.
                // Dla BigDecimal unikalność jest sprawdzana przez equals(),
                // więc np. 10.0 i 10.00 są traktowane jako różne wartości.
                .distinct()
                .toList();
    }

    public List<Transaction> page(
            List<Transaction> transactions,
            int pageNumber,
            int pageSize
    ) {
        validateTransactions(transactions);

        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        // offset to liczba elementów, które trzeba pominąć na początku listy, żeby wejść na daną stronę.
        // oznacza:
        //  pageNumber = 0, pageSize = 10 → offset = 0 → start od pierwszego elementu
        //  pageNumber = 1, pageSize = 10 → offset = 10 → pomiń 10 elementów
        //  pageNumber = 2, pageSize = 10 → offset = 20 → pomiń 20 elementów
        long offset = (long) pageNumber * pageSize;

        // czyli: pomiń offset rekordów, a następnie weź pageSize rekordów.
        // Rzutowanie do long jest po to, żeby uniknąć przepełnienia int przy mnożeniu i pasować do skip(long).
        // limit(pageSize) bierze maksymalnie pageSize elementów ze strumienia po tym,
        // jak wcześniejsze .skip(offset) pominie elementy z poprzednich stron.
        // Przykład: dla pageNumber = 2 i pageSize = 10:
        //   offset = 20
        //   .skip(20) pomija pierwsze 20 transakcji
        //   .limit(10) zwraca kolejne 10 transakcji, czyli stronę nr 2
        return transactions.stream()
                .skip(offset) //  pomiń wcześniejsze rekordy
                .limit(pageSize) // weź tylko tyle rekordów, ile ma mieć strona
                .toList();
    }

    public Optional<Transaction> firstByType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .findFirst();
    }

    public boolean anyAmountAtLeast(
            List<Transaction> transactions,
            BigDecimal minimum
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(minimum, "minimum must not be null");

        return transactions.stream()
                .anyMatch(transaction ->
                        transaction.amount().compareTo(minimum) >= 0
                );

/*
    boolean anyMatch(Predicate<? super T> predicate);

    @FunctionalInterface
    public interface Predicate<T> {
        boolean test (T t);
    }

    Predicate<T> predicate = new Predicate<T>() {
        @Override
        public boolean test(T t) {
            return false;
        }
    };

    Predicate<T> predicate = new Predicate<Transaction>() {
        @Override
        public boolean test(Transaction transaction) {
            return transaction.amount().compareTo(minimum) >= 0;
        }
    };

    lambda:   Predicate<T> predicate = transaction -> transaction.amount().compareTo(minimum) >= 0;

* */
    }

    public boolean allForAccount(
            List<Transaction> transactions,
            String accountNumber
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");

        return transactions.stream()
                .allMatch(transaction ->
                        transaction.accountNumber().equals(accountNumber)
                );
/*
    boolean anyMatch(Predicate<? super T> predicate);

    @FunctionalInterface
    public interface Predicate<T> {
        boolean test (T t);
    }

    Predicate<T> predicate = new Predicate<T>() {
        @Override
        public boolean test(T t) {
            return false;
        }
    };

    Predicate<T> predicate = new Predicate<Transaction>() {
        @Override
        public boolean test(Transaction transaction) {
            return transaction.accountNumber().equals(accountNumber);
        }
    };

    lambda:   Predicate<T> predicate = transaction -> transaction.accountNumber().equals(accountNumber);

 */
    }

    public boolean noneOfType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .noneMatch(transaction -> transaction.type() == type);

        /*
    boolean anyMatch(Predicate<? super T> predicate);

    @FunctionalInterface
    public interface Predicate<T> {
        boolean test (T t);
    }

    Predicate<T> predicate = new Predicate<T>() {
        @Override
        public boolean test(T t) {
            return false;
        }
    };

    Predicate<T> predicate = new Predicate<Transaction>() {
        @Override
        public boolean test(Transaction transaction) {
            return transaction.type() == type;
        }
    };

    lambda:   Predicate<T> predicate = transaction -> transaction.type() == type;

         */
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}