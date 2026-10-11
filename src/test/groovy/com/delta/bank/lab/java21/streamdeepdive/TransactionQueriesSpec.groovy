package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionQueriesSpec extends Specification {

    private final TransactionQueries queries = new TransactionQueries()

    def "sorts by amount and then transaction id"() {
        when:
        def sorted = queries.sortedByAmount(sampleTransactions())

        then:
        sorted*.id() == ["T2", "T4", "T3", "T1", "T5"]
        sorted*.amount() == [
                new BigDecimal("20.00"),
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        ]
    }

    def "distinct uses BigDecimal equals and therefore considers scale"() {
        when:
        def amounts = queries.distinctAmounts(sampleTransactions())

        then:
        amounts == [
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                new BigDecimal("50.00"),
                new BigDecimal("30.00")
        ]
    }

    def "paginates in source order using zero-based page number"() {
        when:

        // page(...) robi paginację listy transakcji.
        // oznacza to:
        //  sampleTransactions() — źródłowa lista: T1, T2, T3, T4, T5
        //    1 — numer strony, liczony od zera
        //    2 — rozmiar strony, czyli po 2 elementy na stronę
        // Więc:
        //    strona 0 => T1, T2
        //    strona 1 => T3, T4
        //    strona 2 => T5
        def page = queries.page(sampleTransactions(), 1, 2)

        then:
        // czyli że metoda zwróciła drugą stronę wyników w oryginalnej kolejności.
        page*.id() == ["T3", "T4"]
    }

    def "returns a shorter last page when there are fewer remaining elements"() {
        when:
        def page = queries.page(sampleTransactions(), 2, 2)

        then:
        page*.id() == ["T5"]
    }

    def "returns an empty page when offset is past the end"() {
        expect:
        queries.page(sampleTransactions(), 10, 2).isEmpty()
    }

    def "findFirst returns the first matching transaction in encounter order"() {
        when:
        def result = queries.firstByType(
                sampleTransactions(),
                TransactionType.DEPOSIT
        )

        then:
        result.isPresent()
        result.get().id() == "T1"
    }

    def "findFirst returns an empty Optional when no transaction matches"() {
        when:
        def result = queries.firstByType(
                sampleTransactions(),
                TransactionType.TRANSFER
        )

        then:
        !result.isPresent()
    }

    def "anyMatch is true when at least one transaction meets the condition"() {
        expect:
        queries.anyAmountAtLeast(
                sampleTransactions(),
                new BigDecimal("100.00")
        )
    }

    def "allMatch is false when one transaction belongs to a different account"() {
        expect:
        !queries.allForAccount(sampleTransactions(), "PLN-1001")
    }

    def "noneMatch is true when no transaction has the requested type"() {
        expect:
        queries.noneOfType(sampleTransactions(), TransactionType.TRANSFER)
    }

    def "match operations use their defined results for an empty list"() {
        expect:
        !queries.anyAmountAtLeast(List.of(), BigDecimal.ONE)
        queries.allForAccount(List.of(), "PLN-1001")
        queries.noneOfType(List.of(), TransactionType.DEPOSIT)
    }

    def "rejects a negative page number"() {
        when:
        queries.page(sampleTransactions(), -1, 2)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "pageNumber must not be negative"
    }

    def "rejects a non-positive page size"() {
        when:
        queries.page(sampleTransactions(), 0, 0)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "pageSize must be positive"
    }

    private static List<Transaction> sampleTransactions() {
        return List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00"),
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00"),
                transaction("T4", "PLN-1002", TransactionType.WITHDRAW, "30.00"),
                transaction("T5", "PLN-1001", TransactionType.DEPOSIT, "100.00")
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