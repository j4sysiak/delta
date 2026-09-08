package com.delta.bank

import com.delta.bank.application.BankAccountService
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

import java.math.BigDecimal

class TransactionHistorySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService bankAccountService

    def "stores account operations in transaction history"() {
        given:
        bankAccountService.openAccount('PLN-2001', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        bankAccountService.deposit('PLN-2001', new BigDecimal('250.00'))
        bankAccountService.withdraw('PLN-2001', new BigDecimal('100.00'))

        then:
        bankAccountService.find('PLN-2001').balance == new BigDecimal('1150.00')
    }
}