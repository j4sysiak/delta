package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionStatisticsSpec extends Specification {

    private final TransactionStatistics statistics = new TransactionStatistics()

    def "counts all transactions"() {
        expect:
        statistics.count(sampleTransactions()) == 5L
    }

    def "finds minimum and maximum amount"() {
        when:
        def minimum = statistics.minimumAmount(sampleTransactions())
        def maximum = statistics.maximumAmount(sampleTransactions())

        then:
        // Nie wywołuj get() bez sprawdzenia, czy Optional zawiera wartość.
        // W produkcyjnym kodzie lepiej wybrać świadome zachowanie:
        //   wartość domyślną, inny typ wyniku albo jawny wyjątek, zgodnie z kontraktem metody.
        minimum.isPresent()
        // minimum.get() wyciąga wartość z Optional — wcześniej test gwarantuje, że ona istnieje przez minimum.isPresent()
        minimum.get().compareTo(new BigDecimal("20.00")) == 0  // Ta linijka sprawdza, czy minimalna kwota transakcji wynosi dokładnie 20.00.
        maximum.isPresent()
        maximum.get().compareTo(new BigDecimal("100.00")) == 0
    }

    def "sums amounts using reduce with identity"() {
        expect:
        statistics.totalAmount(sampleTransactions()) ==
                new BigDecimal("300.00")
    }

    def "sums amounts using reduce without identity"() {
        when:
        def total = statistics.totalAmountIfPresent(sampleTransactions())

        then:
        total.isPresent()
        total.get().compareTo(new BigDecimal("300.00")) == 0
    }

    def "empty stream has a count of zero and no minimum or maximum"() {
        given:
        def empty = List.<Transaction>of()

        expect:
        statistics.count(empty) == 0L
        statistics.minimumAmount(empty).isEmpty()
        statistics.maximumAmount(empty).isEmpty()
    }

    def "identity reduction returns zero for an empty stream"() {
        expect:
        statistics.totalAmount(List.<Transaction>of()) == BigDecimal.ZERO
    }

    def "reduction without identity returns an empty Optional"() {
        expect:
        statistics.totalAmountIfPresent(List.<Transaction>of()).isEmpty()
    }

    def "rejects null transaction list"() {
        when:
        statistics.count(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction in list"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        statistics.totalAmount(transactions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
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