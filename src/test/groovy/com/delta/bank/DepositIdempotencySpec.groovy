package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import org.springframework.beans.factory.annotation.Autowired

class DepositIdempotencySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    def "duplicate deposit request is ignored"() {
        given:
        service.openAccount('PLN-9001', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        def first = service.deposit('dep-9001', 'PLN-9001', new BigDecimal('200.00'))
        def second = service.deposit('dep-9001', 'PLN-9001', new BigDecimal('200.00'))

        then:
        first.balance == new BigDecimal('1200.00')
        second.balance == new BigDecimal('1200.00')
        service.find('PLN-9001').balance == new BigDecimal('1200.00')
    }
}
