package com.delta.bank

import spock.lang.Specification

class BankAccountSpec extends Specification {

    def "deposit increases balance"() {
        given:
        def account = new BankAccount('PLN-1001', 'Alice', Money.of('1000.00', 'PLN'))

        when:
        account.deposit(Money.of('250.50', 'PLN'))

        then:
        account.balance() == Money.of('1250.50', 'PLN')
    }

    def "withdraw fails when funds are insufficient"() {
        given:
        def account = new BankAccount('PLN-1001', 'Alice', Money.of('100.00', 'PLN'))

        when:
        account.withdraw(Money.of('200.00', 'PLN'))

        then:
        def ex = thrown(IllegalStateException)
        ex.message == 'Insufficient funds'
    }
}
