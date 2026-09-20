package com.delta.bank.lab.java21.concurrency

import spock.lang.Specification

class ConcurrentVirtualThreadSpec extends Specification {

    private final DepositRunnerInVirtualThreads runner = new DepositRunnerInVirtualThreads()

    def "safe account preserves all concurrent deposits"() {
        given:
        def account = new ConcurrentBankAccount()
        def depositCount = 1000
        def amount = new BigDecimal("1.00")

        when:
        def finalBalance = runner.runDeposits(
                account,
                depositCount,
                amount
        )

        then:
        finalBalance == new BigDecimal("1000.00")
        account.balance() == new BigDecimal("1000.00")
    }

    def "safe account handles larger deposit amount"() {
        given:
        def account = new ConcurrentBankAccount()
        def depositCount = 250
        def amount = new BigDecimal("10.00")

        when:
        def finalBalance = runner.runDeposits(
                account,
                depositCount,
                amount
        )

        then:
        finalBalance == new BigDecimal("2500.00")
    }

    def "unsafe account demonstrates a race-prone implementation"() {
        given:
        def account = new UnsafeBankAccount()
        def depositCount = 1000
        def amount = new BigDecimal("1.00")

        when:
        def finalBalance = runner.runDeposits(
                account,
                depositCount,
                amount
        )

        then:
        finalBalance <= new BigDecimal("1000.00")
        finalBalance > BigDecimal.ZERO
    }

    def "runner rejects invalid deposit count"() {
        given:
        def account = new ConcurrentBankAccount()

        when:
        runner.runDeposits(
                account,
                0,
                new BigDecimal("1.00")
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "depositCount must be greater than zero"
    }

    def "account rejects non-positive deposit"() {
        given:
        def account = new ConcurrentBankAccount()

        when:
        account.deposit(new BigDecimal("0.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be greater than zero"
    }
}