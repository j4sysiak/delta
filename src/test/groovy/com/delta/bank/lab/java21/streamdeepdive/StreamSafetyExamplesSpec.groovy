package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

import java.util.function.Function

class StreamSafetyExamplesSpec extends Specification {

    private final StreamSafetyExamples examples = new StreamSafetyExamples()

    def "filters before mapping and transforms only matching elements"() {
        given:
        def transactions = sampleTransactions()
        def mappedIds = []


        Function<Transaction, String> idMapper = { Transaction transaction ->
            mappedIds.add(transaction.id())  // — zapisuje id do pomocniczej listy jako efekt uboczny
            transaction.id()  // — zwraca id jako wynik dla .map(...)
                              // Bez tej linii .map(idMapper) zwracałby wynik add(...), czyli true/false, a nie "T1", "T3".
        } as Function<Transaction, String>

        /*
        Można też zapisać w bardziej klasyczny sposób, bez lambdy, ale to jest mniej czytelne:
        Function<Transaction, String> idMapper = { Transaction transaction ->
            def id = transaction.id()
            mappedIds.add(id)
            id
        } as Function<Transaction, String>
        * */

        when:
        // List<String> ids
        def ids = transactions.stream()
                .filter { Transaction transaction ->
                    transaction.type() == TransactionType.DEPOSIT
                }
                // Najpierw filtrujemy DEPOSIT, więc mapper wykona się tylko dla T1 i T3
                // mapuje tylko przefiltrowane transakcje (DEPOSIT)
                .map(idMapper)  //   <R> Stream<R> map( Function<Transaction, String> idMapper
                .toList()

        /*
        .map(idMapper)
        <R> Stream<R> map(Function<? super T, ? extends R> mapper);

        @FunctionalInterface
        public interface Function<T, R> {
          R apply(T t);
        }

        Function<T, R> function = new Function<T, R>() {
            @Override
            public R apply(T t) {
                // implementacja funkcji
            }
        };

        Function<Transaction, String> function = new Function<Transaction, String>() {
           @Override
           public String apply(Transaction t) {
               return t.id();
           }
        };
      Równoważna wersja lambda:
      Function<Transaction, String> function = (Transaction t) -> {
           return t.id();
      };

      W tym teście dokładnie to robi idMapper, tylko dodatkowo zapisuje id do mappedIds przed zwróceniem wyniku.

        Function<Transaction, String> idMapper = { Transaction transaction ->
            mappedIds.add(transaction.id())  // zapisuje id do listy mappedIds[] przed zwróceniem wyniku.
            transaction.id()
        } as Function<Transaction, String>

        as Function<Transaction, String>
        ma sens głównie wtedy, gdy nie ma jawnego typu po lewej, np. przy def idMapper = { ... }
        albo gdy chcesz wymusić konkretny typ w mniej oczywistym miejscu. Tutaj to raczej nadmiarowe, ale poprawne.

        * */


        then:
        ids == ["T1", "T3"]
        mappedIds == ["T1", "T3"]
    }

    def "does not use stream operations to mutate the source"() {
        given:
        def transactions = sampleTransactions()
        def originalIds = transactions*.id()

        when:
        def depositIds = examples.depositIds(transactions)

        then:
        depositIds == ["T1", "T3"]
        transactions*.id() == originalIds
    }

    def "sequential and parallel reductions produce the same total"() {
        when:
        def sequentialTotal = examples.totalSequential(sampleTransactions())
        def parallelTotal = examples.totalParallel(sampleTransactions())

        then:
        sequentialTotal == new BigDecimal("200.00")
        parallelTotal == sequentialTotal
    }

    def "parallel toList preserves encounter order for a list source"() {
        given:
        def transactions = sampleTransactions()

        expect:
        examples.idsInEncounterOrder(transactions) == ["T1", "T2", "T3", "T4"]
    }

    def "returns empty results and zero for an empty input"() {
        given:
        def empty = List.<Transaction>of()

        expect:
        examples.depositIds(empty).isEmpty()
        examples.totalSequential(empty) == BigDecimal.ZERO
        examples.totalParallel(empty) == BigDecimal.ZERO
        examples.idsInEncounterOrder(empty).isEmpty()
    }

    def "rejects null input"() {
        when:
        examples.totalParallel(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    private static List<Transaction> sampleTransactions() {
        return List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00"),
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00"),
                transaction("T4", "PLN-1002", TransactionType.TRANSFER, "30.00")
        )
    }

    private static Transaction transaction(
            String id,
            String accountNumber,
            TransactionType type,
            String amount
    ) {
        return new Transaction(id, accountNumber, type, new BigDecimal(amount))
    }
}