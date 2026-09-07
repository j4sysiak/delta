package com.delta.bank

import com.delta.bank.application.BankAccountService
import org.springframework.transaction.annotation.Transactional

import java.math.BigDecimal

@Transactional
class BankAccountRepositorySpec extends BaseIntegrationSpec {

    def "persists and loads account from postgres"() {
        when:
        def service = applicationContext.getBean(BankAccountService)
        def saved = service.openAccount('PLN-7777', 'Alice', new BigDecimal('1000.00'), 'PLN')
        def loaded = service.find('PLN-7777')

        then:
        loaded.number == saved.number
        loaded.owner == 'Alice'
        loaded.balance == new BigDecimal('1000.00')
        loaded.currency == 'PLN'
    }
}
