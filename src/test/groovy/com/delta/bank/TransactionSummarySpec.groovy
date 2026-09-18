package com.delta.bank

import com.delta.bank.bootstrap.DeltaApplication
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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DeltaApplication
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionSummarySpec extends BaseIntegrationSpec {

    // Klient testowy do wykonywania żądań HTTP wobec kontekstu aplikacji
    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        // Buduje instancję MockMvc opartą na kontekście aplikacji, aby testować endpointy bez uruchamiania serwera HTTP
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "returns transaction summary"() {
        given:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-SUM-01",
                      "owner": "Alice",
                      "balance": "1000.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        mockMvc.perform(post("/accounts/PLN-SUM-01/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "sum-dep-01",
                      "amount": "200.00"
                    }
                '''))
                .andExpect(status().isOk())

        and:
        mockMvc.perform(post("/accounts/PLN-SUM-01/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "sum-wd-01",
                      "amount": "50.00"
                    }
                '''))
                .andExpect(status().isOk())

        expect:
        mockMvc.perform(get("/accounts/PLN-SUM-01/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.accountNumber').value('PLN-SUM-01'))
                .andExpect(jsonPath('$.transactionCount').value(3))
                .andExpect(jsonPath('$.totalDeposits').value(1200.0))
                .andExpect(jsonPath('$.totalWithdrawals').value(50.0))
                .andExpect(jsonPath('$.netChange').value(1150.0))
    }
}