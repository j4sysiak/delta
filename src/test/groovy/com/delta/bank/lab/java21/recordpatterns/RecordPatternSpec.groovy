package com.delta.bank.lab.java21.recordpatterns

import spock.lang.Specification

class RecordPatternSpec extends Specification {

    private final RecordPatternProcessor processor = new RecordPatternProcessor()

    def "rozpakowuje zagniezdzone rekordy przez instanceof"() {
        given:
        def command = new DepositCommand(
                new AccountReference("PLN-1001"),
                new Money(new BigDecimal("150.00"), "PLN")
        )

        expect:
        processor.describeUsingInstanceof(command) ==
                "ACCOUNT PLN-1001 HAS DEPOSIT 150.00 PLN"
    }

    def "rozpakowuje zagniezdzone rekordy przez switch"() {
        given:
        def command = new DepositCommand(
                new AccountReference("PLN-1001"),
                new Money(new BigDecimal("250.00"), "PLN")
        )

        expect:
        processor.describeUsingSwitch(command) ==
                "DEPOSIT 250.00 PLN TO PLN-1001"
    }

    def "odczytuje konto kwotę i walutę przez record pattern"() {
        given:
        def command = new DepositCommand(
                new AccountReference("EUR-2001"),
                new Money(new BigDecimal("99.99"), "EUR")
        )

        expect:
        processor.describeAccountAndMoney(command) ==
                "ACCOUNT=EUR-2001, AMOUNT=99.99, CURRENCY=EUR"
    }

    def "obsługuje null w switch"() {
        expect:
        processor.describeUsingSwitch(null) == "NO COMMAND"
    }

    def "rekordy porównują wartości komponentów"() {
        given:
        def first = new DepositCommand(
                new AccountReference("PLN-1001"),
                new Money(new BigDecimal("100.00"), "PLN")
        )

        def second = new DepositCommand(
                new AccountReference("PLN-1001"),
                new Money(new BigDecimal("100.00"), "PLN")
        )

        expect:
        first == second
        first.account().number() == "PLN-1001"
        first.money().amount() == new BigDecimal("100.00")
        first.money().currency() == "PLN"
    }
}