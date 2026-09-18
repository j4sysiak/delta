package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.domain.AccountTransactionRepository
import com.delta.bank.domain.TransactionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class TransactionHistorySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    AccountTransactionRepository transactionRepository

    def "stores deposit and withdrawal events for account"() {
        given:
        service.openAccount('PLN-5001', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        service.deposit('dep-5001', 'PLN-5001', new BigDecimal('250.00'))
        service.withdraw('wd-5001', 'PLN-5001', new BigDecimal('100.00'))


        def of = PageRequest.of(0, 10)
        then:
        def transactions = transactionRepository
                .findByAccountNumberOrderByCreatedAtDesc('PLN-5001', of)

        transactions.size() == 3
        transactions[0].type == TransactionType.WITHDRAW
        transactions[1].type == TransactionType.DEPOSIT
        transactions[2].type == TransactionType.DEPOSIT
    }
}