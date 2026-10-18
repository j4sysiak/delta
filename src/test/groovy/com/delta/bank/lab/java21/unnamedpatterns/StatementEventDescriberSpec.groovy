package com.delta.bank.lab.java21.unnamedpatterns

import spock.lang.Specification
import spock.lang.Unroll

class StatementEventDescriberSpec extends Specification {

    private final StatementEventDescriber describer =
            new StatementEventDescriber()

    @Unroll
    def "describes event as #expected"() {
        expect:
        describer.describe(event) == expected

        where:
        event                                                                | expected
        new DepositEvent("PLN-1001", new BigDecimal("250.00"))               | "Deposit: 250.00 PLN"
        new DepositEvent("PLN-1001", new BigDecimal("1000.00"))              | "Large deposit: 1000.00 PLN"
        new WithdrawalEvent("PLN-1002", new BigDecimal("80.00"))             | "Withdrawal: 80.00 PLN"
        new WithdrawalEvent("PLN-1002", new BigDecimal("1500.00"))           | "Large withdrawal: 1500.00 PLN"
        new TransferEvent("PLN-1001", "PLN-1002", new BigDecimal("75.00"))   | "Transfer: 75.00 PLN"
        null                                                                 | "No statement event"
    }

    def "validates record components even when patterns ignore them"() {
        when:
        new DepositEvent(" ", new BigDecimal("10.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }
}