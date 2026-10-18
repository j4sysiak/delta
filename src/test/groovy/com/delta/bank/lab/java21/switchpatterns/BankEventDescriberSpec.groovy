package com.delta.bank.lab.java21.switchpatterns

import spock.lang.Specification
import spock.lang.Unroll

class BankEventDescriberSpec extends Specification {

    private final BankEventDescriber describer = new BankEventDescriber()

    @Unroll
    def "describes #event"() {
        expect:
        describer.describe(event) == expected

        where:
        event                                                                 | expected
        new DepositRecorded("PLN-1001", new BigDecimal("250.00"))             | "Deposit to PLN-1001: 250.00 PLN"
        new DepositRecorded("PLN-1001", new BigDecimal("1000.00"))            | "Large deposit to PLN-1001: 1000.00 PLN"
        new WithdrawalRecorded("PLN-1002", new BigDecimal("80.00"))           | "Withdrawal from PLN-1002: 80.00 PLN"
        new WithdrawalRecorded("PLN-1002", new BigDecimal("1500.00"))         | "Large withdrawal from PLN-1002: 1500.00 PLN"
        new TransferRecorded("PLN-1001", "PLN-1002", new BigDecimal("75.00")) | "Transfer from PLN-1001 to PLN-1002: 75.00 PLN"
        null                                                                  | "No bank event"
    }

    def "rejects blank account number"() {
        when:
        new DepositRecorded(" \t ", new BigDecimal("10.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }

    def "rejects non-positive transfer amount"() {
        when:
        new TransferRecorded(
                "PLN-1001",
                "PLN-1002",
                new BigDecimal("0.00")
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be positive"
    }
}