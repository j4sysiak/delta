package com.delta.bank.lab.java21.streams

import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Stream

class DepositAnalyzerSpec extends Specification {

    def "List.of preserves order and rejects modification"() {
        given:
        def deposits = List.of(
                new BigDecimal("100.00"),
                new BigDecimal("50.00")
        )

        expect:
        deposits == [new BigDecimal("100.00"), new BigDecimal("50.00")]

        when:
        deposits.add(new BigDecimal("10.00"))

        then:
        thrown(UnsupportedOperationException)
    }

    def "List.of rejects null"() {
        when:
        List.of("DEPOSIT", null)

        then:
        thrown(NullPointerException)
    }

    def "Map.of provides labels and rejects modification"() {
        given:
        def labels = Map.of(
                "DEPOSIT", "Wplata",
                "WITHDRAW", "Wyplata"
        )

        expect:
        labels.get("DEPOSIT") == "Wplata"
        labels.get("WITHDRAW") == "Wyplata"
        labels.size() == 2

        when:
        labels.put("TRANSFER", "Przelew")

        then:
        thrown(UnsupportedOperationException)
    }

    def "Map.of rejects duplicate keys"() {
        when:
        Map.of("DEPOSIT", "Wplata", "DEPOSIT", "Inna etykieta")

        then:
        thrown(IllegalArgumentException)
    }

    def "filters deposits preserving order and returns an unmodifiable list"() {
        given:
        def analyzer = new DepositAnalyzer()
        def deposits = List.of(
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00")
        )

        when:
        def selected = analyzer.atLeast(deposits, new BigDecimal("100.00"))

        then:
        selected == [new BigDecimal("200.00"), new BigDecimal("100.00")]

        when:
        selected.add(new BigDecimal("300.00"))

        then:
        thrown(UnsupportedOperationException)
    }

    def "Stream.toList result does not follow structural changes to the source"() {
        given:
        def source = new ArrayList<String>(["DEPOSIT", "WITHDRAW"])

        when:
        def result = source.stream().toList()
        source.add("TRANSFER")

        then:
        result == ["DEPOSIT", "WITHDRAW"]
        source == ["DEPOSIT", "WITHDRAW", "TRANSFER"]
    }

    def "Stream.toList allows null elements"() {
        when:
        def result = Stream.of("DEPOSIT", null).toList()

        then:
        result.size() == 2
        result.get(0) == "DEPOSIT"
        result.get(1) == null
    }

    def "summarizes count and total using teeing"() {
        given:
        def analyzer = new DepositAnalyzer()
        def deposits = List.of(
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                new BigDecimal("50.00")
        )

        when:
        def summary = analyzer.summarize(deposits)

        then:
        summary.count() == 3L
        summary.total() == new BigDecimal("350.00")
    }

    def "summarizes an empty list"() {
        when:
        def summary = new DepositAnalyzer().summarize(List.of())

        then:
        summary.count() == 0L
        summary.total() == BigDecimal.ZERO
    }

    @Unroll
    def "rejects non-positive deposit #amount"() {
        when:
        new DepositAnalyzer().summarize(List.of(new BigDecimal(amount)))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "deposit must be positive"

        where:
        amount << ["0.00", "-10.00"]
    }

    def "rejects null deposits list"() {
        when:
        new DepositAnalyzer().summarize(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "deposits must not be null"
    }

    def "rejects null deposit inside a list"() {
        given:
        def deposits = new ArrayList<BigDecimal>()
        deposits.add(null)

        when:
        new DepositAnalyzer().summarize(deposits)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "deposit must not be null"
    }

    def "rejects null minimum"() {
        when:
        new DepositAnalyzer().atLeast(List.of(), null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "minimum must not be null"
    }
}