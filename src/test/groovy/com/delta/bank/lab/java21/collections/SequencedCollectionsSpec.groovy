package com.delta.bank.lab.java21.collections

import spock.lang.Specification

class SequencedCollectionsSpec extends Specification {

    private final SequencedCollectionsLab lab = new SequencedCollectionsLab()

    def "returns first and last transaction from a sequenced collection"() {
        given:
        def transactions = new ArrayList<>([
                "OPEN",
                "DEPOSIT",
                "WITHDRAW"
        ])

        expect:
        lab.first(transactions) == "OPEN"
        lab.last(transactions) == "WITHDRAW"
    }

    def "returns transactions in reverse order without changing the original list"() {
        given:
        def transactions = new ArrayList<>([
                "OPEN",
                "DEPOSIT",
                "WITHDRAW"
        ])

        when:
        def reversedCopy = lab.reversedCopy(transactions)

        then:
        reversedCopy == ["WITHDRAW", "DEPOSIT", "OPEN"]
        transactions == ["OPEN", "DEPOSIT", "WITHDRAW"]
    }

    def "reversed collection is a view backed by the original list"() {
        given:
        def transactions = new ArrayList<>([
                "OPEN",
                "DEPOSIT",
                "WITHDRAW"
        ])
        def reversedView = lab.reversedView(transactions)

        when:
        transactions.add("TRANSFER")

        then:
        new ArrayList<>(reversedView) ==
                ["TRANSFER", "WITHDRAW", "DEPOSIT", "OPEN"]
    }

    def "returns first and last values from a sequenced set"() {
        given:
        def transactionTypes = new LinkedHashSet<String>()
        transactionTypes.add("DEPOSIT")
        transactionTypes.add("WITHDRAW")
        transactionTypes.add("TRANSFER")

        expect:
        lab.first(transactionTypes) == "DEPOSIT"
        lab.last(transactionTypes) == "TRANSFER"
    }

    def "returns first and last entries from a sequenced map"() {
        given:
        def accountStatuses = new LinkedHashMap<String, String>()
        accountStatuses.put("PLN-1001", "ACTIVE")
        accountStatuses.put("EUR-2001", "BLOCKED")

        when:
        def firstEntry = lab.firstEntry(accountStatuses)
        def lastEntry = lab.lastEntry(accountStatuses)

        then:
        firstEntry.key == "PLN-1001"
        firstEntry.value == "ACTIVE"
        lastEntry.key == "EUR-2001"
        lastEntry.value == "BLOCKED"
    }

    def "reversed map view iterates from the last entry to the first"() {
        given:
        def accountStatuses = new LinkedHashMap<String, String>()
        accountStatuses.put("PLN-1001", "ACTIVE")
        accountStatuses.put("EUR-2001", "BLOCKED")

        when:
        def reversedView = lab.reversedMapView(accountStatuses)

        then:
        new ArrayList<>(reversedView.entrySet()) == [
                new AbstractMap.SimpleEntry("EUR-2001", "BLOCKED"),
                new AbstractMap.SimpleEntry("PLN-1001", "ACTIVE")
        ]
        accountStatuses.keySet().toList() == ["PLN-1001", "EUR-2001"]
    }

    def "throws when getting the first element of an empty collection"() {
        given:
        def transactions = new ArrayList<String>()

        when:
        lab.first(transactions)

        then:
        thrown(NoSuchElementException)
    }

    def "throws when getting the last element of an empty collection"() {
        given:
        def transactions = new ArrayList<String>()

        when:
        lab.last(transactions)

        then:
        thrown(NoSuchElementException)
    }
}