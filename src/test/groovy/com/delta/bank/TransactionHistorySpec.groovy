package com.delta.bank

import com.delta.bank.application.BankAccountService
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired

class TransactionHistorySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    AccountTransactionRepository transactionRepository

    def "stores deposit and withdrawal events for account"() {
        given:
        service.openAccount('PLN-5001', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        service.deposit('PLN-5001', new BigDecimal('250.00'))
        service.withdraw('PLN-5001', new BigDecimal('100.00'))

        then:
        def transactions = transactionRepository.findByAccountNumberOrderByCreatedAtDesc('PLN-5001')
        transactions.size() == 3
        transactions[0].type == TransactionType.WITHDRAW
        transactions[1].type == TransactionType.DEPOSIT
        transactions[2].type == TransactionType.DEPOSIT
    }
}