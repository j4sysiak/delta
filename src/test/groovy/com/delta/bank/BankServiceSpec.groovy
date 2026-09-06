package com.delta.bank

import spock.lang.Specification

class BankServiceSpec extends Specification {

    def service = new BankService()

    def "transfer moves money between accounts"() {
        given:
        def alice = new BankAccount('PLN-1001', 'Alice', Money.of('1000.00', 'PLN'))
        def bob = new BankAccount('PLN-2002', 'Bob', Money.of('250.00', 'PLN'))

        when:
        service.transfer(alice, bob, Money.of('150.00', 'PLN'))

        then:
        alice.balance() == Money.of('850.00', 'PLN')
        bob.balance() == Money.of('400.00', 'PLN')
    }

    def "routeEvent supports pattern matching"() {
        expect:
        service.routeEvent(event) == expected

        where:
        event                                            | expected
        new BankAccount('PLN-1001', 'Alice', Money.of('10.00', 'PLN')) | 'ACCOUNT:PLN-1001'
        Money.of('50.00', 'PLN')                          | 'MONEY:50.00 PLN'
        'invoice'                                        | 'TEXT:invoice'
        null                                             | 'NO_EVENT'
    }
}
