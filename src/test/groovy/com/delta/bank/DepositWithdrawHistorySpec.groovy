package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepositWithdrawHistorySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "deposit and withdraw history is returned over http"() {
        given:
        service.openAccount('PLN-9201', 'Alice', new BigDecimal('1000.00'), 'PLN')

        and:
        def depositPayload = """
            {
              "requestId": "dep-9201",
              "amount": "200.00"
            }
        """
        def withdrawPayload = """
            {
              "requestId": "wd-9201",
              "amount": "50.00"
            }
        """

        when:
        mockMvc.perform(post("/accounts/PLN-9201/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(depositPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(1200.0))

        and:
        mockMvc.perform(post("/accounts/PLN-9201/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withdrawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(1150.0))

        then:
        mockMvc.perform(get("/accounts/PLN-9201/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content[0].createdAt').exists())
                .andExpect(jsonPath('$.content[0].updatedAt').exists())
                .andExpect(jsonPath('$.content[?(@.transferRequestId == "dep-9201")]').value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath('$.content[?(@.transferRequestId == "wd-9201")]').value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath('$.content[?(@.type == "DEPOSIT")]').value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath('$.content[?(@.type == "WITHDRAW")]').value(org.hamcrest.Matchers.hasSize(1)))
    }
}
