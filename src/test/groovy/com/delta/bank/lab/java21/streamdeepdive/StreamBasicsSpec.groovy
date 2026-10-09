package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

import java.util.function.Predicate

class StreamBasicsSpec extends Specification {

    def "extracts deposit amounts without changing the source"() {
        given:
        def transactions = sampleTransactions()
        def basics = new StreamBasics()

        when:
        def amounts = basics.depositAmounts(transactions)

        then:
        amounts == [new BigDecimal("100.00"), new BigDecimal("50.00")]
        transactions.size() == 4
        transactions.get(1).type() == TransactionType.WITHDRAW
    }

    def "does not execute filter until a terminal operation is called"() {
        given:
        def visitedIds = []
        Predicate<Transaction> predicate = { Transaction transaction ->
            visitedIds.add(transaction.id())
            transaction.type() == TransactionType.DEPOSIT
        } as Predicate<Transaction>

        /*
        zapis javowy:
        Predicate<Transaction> predicate = new Predicate<>() {
            @Override
            public boolean test(Transaction transaction) {
                visitedIds.add(transaction.id());
                return transaction.type() == TransactionType.DEPOSIT;
            }
        };


        Predicate<Transaction> predicate = transaction -> {
           visitedIds.add(transaction.id());
           return transaction.type() == TransactionType.DEPOSIT;
       };



        * */

        when:
        // czyli w momencie wywołania stream.filter(predicate)
        // nie wykonuje się jeszcze test, tylko dopiero przy terminalnej operacji (np. toList()).
        def stream = sampleTransactions().stream().filter(predicate)

        then:
        visitedIds.isEmpty()

        when:
        def deposits = stream.toList()

        then:
        deposits.collect { it.id() } == ["T1", "T3"]
        visitedIds == ["T1", "T2", "T3", "T4"]
    }

    def "cannot reuse a stream after a terminal operation"() {
        given:
        def stream = sampleTransactions().stream()

        when:
        def firstResult = stream.toList()

        then:
        firstResult.size() == 4

        when:
        stream.toList()

        then:
        thrown(IllegalStateException) // bo stream został już zamknięty po wywołaniu terminalnej operacji toList()
    }

    def "can create a new stream from the same list"() {
        given:
        def transactions = sampleTransactions()

        when:
        def firstResult = transactions.stream().toList()
        def secondResult = transactions.stream().toList()

        then:
        firstResult == secondResult
    }

    def "returns an empty result for an empty source"() {
        expect:
        new StreamBasics().depositAmounts(List.of()).isEmpty()
    }

    def "rejects a null source"() {
        when:
        new StreamBasics().depositAmounts(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects a null transaction"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        new StreamBasics().depositAmounts(transactions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
    }

    private static List<Transaction> sampleTransactions() {
        // wywołanie List.of(...) powoduje uruchomienie predykatu test()
        // to znaczy, wybierze tylko te transakcje, które spełniają warunek w predykacie (czyli są typu DEPOSIT) - test drugi
        return List.of(
                new Transaction("T1", "PLN-1001", TransactionType.DEPOSIT,
                        new BigDecimal("100.00")),
                new Transaction("T2", "PLN-1001", TransactionType.WITHDRAW,
                        new BigDecimal("20.00")),
                new Transaction("T3", "PLN-1002", TransactionType.DEPOSIT,
                        new BigDecimal("50.00")),
                new Transaction("T4", "PLN-1002", TransactionType.TRANSFER,
                        new BigDecimal("30.00"))
        )
    }
}