package com.delta.bank.lab.java21.switchexpressions

import spock.lang.Specification
import spock.lang.Unroll

class BankFeeCalculatorSpec extends Specification {

    @Unroll
    def "calculates fee #expected for #operation and amount #amount"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        def fee = calculator.calculate(operation, new BigDecimal(amount))

        then:
        fee == new BigDecimal(expected)
        fee.scale() == 2

        where:
        operation                  | amount    | expected
        BankOperationType.DEPOSIT  | "1000.00" | "0.00"
        BankOperationType.WITHDRAW | "1000.00" | "1.00"
        BankOperationType.TRANSFER | "100.00"  | "2.00"
        BankOperationType.TRANSFER | "400.00"  | "2.00"
        BankOperationType.TRANSFER | "1000.00" | "5.00"
        BankOperationType.TRANSFER | "1001.00" | "5.01"
    }

    @Unroll
    def "rejects non-positive amount #amount"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        calculator.calculate(BankOperationType.TRANSFER, new BigDecimal(amount))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be positive"

        where:
        amount << ["0.00", "-10.00"]
    }

    @Unroll
    def "rejects null #argument"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        calculator.calculate(operation, amount)

        then:
        def exception = thrown(NullPointerException)
        exception.message == message

        where:
        argument    | operation                 | amount                  | message
        "operation" | null                      | new BigDecimal("10.00") | "operation must not be null"
        "amount"    | BankOperationType.DEPOSIT | null                    | "amount must not be null"
    }
}