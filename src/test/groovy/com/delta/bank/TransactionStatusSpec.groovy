package com.delta.bank

import com.delta.bank.application.BankAccountService
import com.delta.bank.domain.TransactionStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class TransactionStatusSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "deposit and withdraw transactions are posted and visible in history"() {
        given:
        service.openAccount('PLN-9901', 'Alice', new BigDecimal('1000.00'), 'PLN')

        when:
        service.deposit('dep-9901', 'PLN-9901', new BigDecimal('200.00'))
        service.withdraw('wd-9901', 'PLN-9901', new BigDecimal('50.00'))

        then:
        mockMvc.perform(get("/accounts/PLN-9901/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content[?(@.type == "DEPOSIT")]').value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath('$.content[?(@.type == "DEPOSIT")].status')
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(TransactionStatus.POSTED.name()))))
                .andExpect(jsonPath('$.content[?(@.type == "WITHDRAW")].status')
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(TransactionStatus.POSTED.name()))))
    }
}