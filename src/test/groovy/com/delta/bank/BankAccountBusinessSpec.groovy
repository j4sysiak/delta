package com.delta.bank

import com.delta.bank.application.BankAccountService
import org.springframework.beans.factory.annotation.Autowired

class BankAccountBusinessSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    def "deposit increases balance"() {
        given:
        service.openAccount('PLN-1001', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        def account = service.deposit('PLN-1001', new BigDecimal('250.50'))

        then:
        account.balance == new BigDecimal('1250.50')
    }

    def "withdraw fails when funds are insufficient"() {
        given:
        service.openAccount('PLN-1002', 'Bob', new BigDecimal('100.00'), 'PLN')

        when:
        service.withdraw('PLN-1002', new BigDecimal('200.00'))

        then:
        IllegalStateException ex = thrown()
        ex.message == 'Insufficient funds'
    }

    def "transfer moves funds between accounts"() {
        given:
        service.openAccount('PLN-1003', 'Alice', new BigDecimal('1000.00'), 'PLN')
        service.openAccount('PLN-1004', 'Bob', new BigDecimal('250.00'), 'PLN')

        when:
        service.transfer('PLN-1003', 'PLN-1004', new BigDecimal('150.00'))

        then:
        service.find('PLN-1003').balance == new BigDecimal('850.00')
        service.find('PLN-1004').balance == new BigDecimal('400.00')
    }
}