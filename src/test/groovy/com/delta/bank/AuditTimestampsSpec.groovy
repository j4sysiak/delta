package com.delta.bank

import com.delta.bank.domain.BankAccountRepository
import org.springframework.beans.factory.annotation.Autowired

class AuditTimestampsSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountRepository bankAccountRepository

    def "sets createdAt and updatedAt on account"() {
        given:
        def accountNumber = 'PLN-9001'

        when:
        bankAccountRepository.save(new com.delta.bank.domain.BankAccountEntity(accountNumber, 'Alice', new BigDecimal('1000.00'), 'PLN'))
        def saved = bankAccountRepository.findById(accountNumber).orElseThrow()

        then:
        saved.createdAt != null
        saved.updatedAt != null
        saved.createdAt <= saved.updatedAt
    }

    def "updatedAt changes after balance update"() {
        given:
        def accountNumber = 'PLN-9002'
        bankAccountRepository.save(new com.delta.bank.domain.BankAccountEntity(accountNumber, 'Alice', new BigDecimal('1000.00'), 'PLN'))
        def before = bankAccountRepository.findById(accountNumber).orElseThrow()
        Thread.sleep(1100)

        when:
        before.setBalance(new BigDecimal('1250.00'))
        bankAccountRepository.save(before)
        def after = bankAccountRepository.findById(accountNumber).orElseThrow()

        then:
        after.updatedAt != null
        after.updatedAt.isAfter(before.updatedAt)
    }
}