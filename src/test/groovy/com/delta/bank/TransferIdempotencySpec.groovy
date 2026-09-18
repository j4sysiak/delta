package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.domain.AccountTransactionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest;

class TransferIdempotencySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    @Autowired
    AccountTransactionRepository transactionRepository

    def "duplicate transfer request is ignored"() {
        given:
        service.openAccount('PLN-7001', 'Alice', new BigDecimal('1000.00'), 'PLN')
        service.openAccount('PLN-7002', 'Bob', new BigDecimal('0.00'), 'PLN')

        when:
        def first = service.transfer('req-123', 'PLN-7001', 'PLN-7002', new BigDecimal('250.00'))
        def second = service.transfer('req-123', 'PLN-7001', 'PLN-7002', new BigDecimal('250.00'))

        then:
       // Pierwsze wywołanie powinno zostać przetworzone, a zduplikowane odrzucone.
        first
        !second
        service.find('PLN-7001').balance == new BigDecimal('750.00')
        service.find('PLN-7002').balance == new BigDecimal('250.00')

        // Weryfikuje, że dla konta nadawcy 'PLN-7001' zapisano tylko jedną transakcję dla tego samego `transferRequestId`.
        final def of = PageRequest.of(0, 10)
        transactionRepository.findByAccountNumberOrderByCreatedAtDesc('PLN-7001', of)
                .count { it.transferRequestId == 'req-123' } == 1

        // Weryfikuje, że dla konta odbiorcy 'PLN-7002' zapisano tylko jedną transakcję dla tego samego `transferRequestId`.
        transactionRepository.findByAccountNumberOrderByCreatedAtDesc('PLN-7002', of)
                .count { it.transferRequestId == 'req-123' } == 1
    }
}
