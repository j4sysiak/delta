package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import org.springframework.beans.factory.annotation.Autowired

class WithdrawIdempotencySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    def "duplicate withdraw request is ignored"() {
        given:
        service.openAccount('PLN-9101', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        def first = service.withdraw('wd-9101', 'PLN-9101', new BigDecimal('200.00'))
        def second = service.withdraw('wd-9101', 'PLN-9101', new BigDecimal('200.00'))

        then:
        first.balance == new BigDecimal('800.00')
        second.balance == new BigDecimal('800.00')
        service.find('PLN-9101').balance == new BigDecimal('800.00')
    }
}
