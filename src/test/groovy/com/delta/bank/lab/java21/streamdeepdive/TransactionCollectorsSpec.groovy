package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionCollectorsSpec extends Specification {

    private final TransactionCollectors collectors = new TransactionCollectors()

    def "indexes transactions by unique id"() {
        when:
        def result = collectors.byId(sampleTransactions())

        then:
        result.size() == 4
        result.get("T2").accountNumber() == "PLN-1001"
        result.get("T2").type() == TransactionType.WITHDRAW
    }

    def "rejects duplicate ids instead of silently discarding a transaction"() {
        given:
        def transactions = List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T1", "PLN-1002", TransactionType.DEPOSIT, "50.00")
        )

        /*
        Dla Collectors.toMap(...) zduplikowany klucz bez funkcji merge kończy się IllegalStateException.
        W tym teście to jest zachowanie celowe:
         - pierwszy rekord ma klucz T1
         - drugi rekord też ma klucz T1
        przy próbie wstawienia drugiego elementu do mapy collector wykrywa konflikt i rzuca wyjątek
        * */
        when:
        collectors.byId(transactions)

        then:
        thrown(IllegalStateException)
    }

    def "merges repeated account keys by adding amounts"() {
        when:
        def totals = collectors.totalsByAccount(sampleTransactions())

        then:
        totals.size() == 2
        totals.get("PLN-1001") == new BigDecimal("120.00")
        totals.get("PLN-1002") == new BigDecimal("80.00")
    }

    def "groups transactions by account preserving order within each group"() {
        when:
        def groups = collectors.byAccount(sampleTransactions())

        then:
        groups.keySet() == ["PLN-1001", "PLN-1002"].toSet()
        groups.get("PLN-1001")*.id() == ["T1", "T2"]
        groups.get("PLN-1002")*.id() == ["T3", "T4"]
    }

    def "partitions deposits and other transactions"() {
        when:
        def partition = collectors.depositsAndOthers(sampleTransactions())

        then:
        partition.get(true)*.id() == ["T1", "T3"]
        partition.get(false)*.id() == ["T2", "T4"]
    }

    def "maps transactions to amounts within each account group"() {
        when:
        // Map<String, List<BigDecimal>> amounts
        def amounts = collectors.amountsByAccount(sampleTransactions())

        then:
        amounts.get("PLN-1001") == [
                new BigDecimal("100.00"),
                new BigDecimal("20.00")
        ]
        amounts.get("PLN-1002") == [
                new BigDecimal("50.00"),
                new BigDecimal("30.00")
        ]
    }

    def "computes count and total for each account group"() {
        when:
        // Map<String, TransactionGroupSummary> summaries
        def summaries = collectors.summariesByAccount(sampleTransactions())

        then:
        summaries.size() == 2
        summaries.get("PLN-1001").count() == 2L
        summaries.get("PLN-1001").total() == new BigDecimal("120.00")
        summaries.get("PLN-1002").count() == 2L
        summaries.get("PLN-1002").total() == new BigDecimal("80.00")
    }

    def "empty input produces no account groups but both partition keys"() {
        given:
        def empty = List.of()

        expect:
        collectors.byId(empty).isEmpty()
        collectors.totalsByAccount(empty).isEmpty()
        collectors.byAccount(empty).isEmpty()
        collectors.amountsByAccount(empty).isEmpty()
        collectors.summariesByAccount(empty).isEmpty()

        when:
        def partition = collectors.depositsAndOthers(empty)

        then:
        partition.keySet() == [true, false].toSet()
        partition.get(true).isEmpty()
        partition.get(false).isEmpty()
    }

    def "rejects null input"() {
        when:
        collectors.byAccount(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        collectors.summariesByAccount(transactions)

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