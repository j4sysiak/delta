package com.delta.bank.lab.java21.patternmatching

import spock.lang.Specification

class BankOperationSpec extends Specification {

    private final BankOperationProcessor processor = new BankOperationProcessor()

    def "processes deposit with pattern matching switch"() {
        given:
        def operation = new DepositOperation(
                "PLN-1001",
                new BigDecimal("150.00"),
                "PLN"
        )

        expect:
        processor.process(operation) == "DEPOSIT 150.00 PLN TO PLN-1001"
    }

    def "processes withdraw with pattern matching switch"() {
        given:
        def operation = new WithdrawOperation(
                "PLN-1001",
                new BigDecimal("50.00"),
                "PLN"
        )

        expect:
        processor.process(operation) == "WITHDRAW 50.00 PLN FROM PLN-1001"
    }

    def "processes transfer with pattern matching switch"() {
        given:
        def operation = new TransferOperation(
                "PLN-1001",
                "PLN-2001",
                new BigDecimal("250.00"),
                "PLN"
        )

        expect:
        processor.process(operation) == "TRANSFER 250.00 PLN FROM PLN-1001 TO PLN-2001"
    }

    def "recognizes operation using pattern matching instanceof"() {
        given:
        def operation = new DepositOperation(
                "PLN-1001",
                new BigDecimal("100.00"),
                "PLN"
        )

        expect:
        processor.describeUsingInstanceof(operation) == "Deposit for account PLN-1001"
    }

    def "rejects null operation"() {
        when:
        processor.process(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "operation must not be null"
    }

    def "records compare by values"() {
        given:
        def first = new DepositOperation(
                "PLN-1001",
                new BigDecimal("100.00"),
                "PLN"
        )
        def second = new DepositOperation(
                "PLN-1001",
                new BigDecimal("100.00"),
                "PLN"
        )

        expect:
        first == second
        first.accountNumber() == "PLN-1001"
        first.amount() == new BigDecimal("100.00")
        first.currency() == "PLN"
    }
}