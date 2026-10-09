package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class StreamTransformationsSpec extends Specification {

    private final StreamTransformations transformations = new StreamTransformations()

    def "filter selects matching transactions and preserves their order"() {
        given:
        def transactions = sampleTransactions()

        when:
        def deposits = transformations.filterByType(
                transactions,
                TransactionType.DEPOSIT
        )

        then:
        /*
        Ta gwiazdka to w Groovy tzw. spread operator.
        deposits*.id() znaczy: wywołaj id() na każdym elemencie listy deposits i zbierz wyniki do nowej listy.
        jest skrótem mniej więcej tego: deposits.collect { it.id() } == ["T1", "T3"]
        Jeśli deposits zawiera 2 transakcje, np. T1 i T3, to deposits*.id() da: ["T1", "T3"]

        * */
        deposits*.id() == ["T1", "T3"]
        /*
        Znaczenie:
           every { ... } — zwraca true, jeśli wszystkie elementy kolekcji spełniają warunek
           it — bieżący element listy, tutaj pojedyncza transakcja
           it.type() — wywołanie metody type() na transakcji
           == TransactionType.DEPOSIT — sprawdzenie, czy typ to DEPOSIT
        Czyli po ludzku: „wszystkie transakcje w deposits są depozytami”.
        * */
        deposits.every { it.type() == TransactionType.DEPOSIT }
        transactions*.id() == ["T1", "T2", "T3", "T4"]
    }

    def "map extracts one amount from each matching transaction"() {
        when:
        def amounts = transformations.amountsForType(
                sampleTransactions(),
                TransactionType.DEPOSIT
        )

        then:
        amounts == [new BigDecimal("100.00"), new BigDecimal("50.00")]
    }

    def "flatMap flattens histories preserving history and transaction order"() {
        given:
        def firstHistory = [
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00")
        ]
        def secondHistory = [
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00")
        ]

        when:
        def flattened = transformations.flattenHistories(
                [firstHistory, secondHistory]
        )

        then:
        flattened*.id() == ["T1", "T2", "T3"]
    }

    def "flatMap treats an empty history as zero transactions"() {
        given:
        def history = [
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00")
        ]

        expect:
                                                                // *.id() — to spread operator: wywołaj id() na każdym elemencie wynikowej listy i zbierz wyniki.
        transformations.flattenHistories([history, [], history])*.id() == ["T1", "T1"]
    }

    def "filter returns an empty list when no transaction matches"() {
        expect:
        transformations.filterByType(
                List.of(transaction(
                        "T1",
                        "PLN-1001",
                        TransactionType.DEPOSIT,
                        "100.00"
                )),
                TransactionType.TRANSFER
        ).isEmpty()
    }

    def "rejects null transaction list"() {
        when:
        transformations.filterByType(null, TransactionType.DEPOSIT)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction type"() {
        when:
        transformations.filterByType(sampleTransactions(), null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "type must not be null"
    }

    def "rejects null history"() {
        given:
        def histories = new ArrayList<List<Transaction>>()
        histories.add(null)

        when:
        transformations.flattenHistories(histories)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "history must not be null"
    }

    def "rejects null transaction inside a history"() {
        given:
        def history = new ArrayList<Transaction>()
        history.add(null)

        when:
        transformations.flattenHistories([history])

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
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