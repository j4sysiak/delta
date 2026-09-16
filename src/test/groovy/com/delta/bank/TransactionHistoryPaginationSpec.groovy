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
class TransactionHistoryPaginationSpec extends BaseIntegrationSpec {

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "history supports pagination"() {
        given:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-PAGE-01",
                      "owner": "Alice",
                      "balance": "1000.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        mockMvc.perform(post("/accounts/PLN-PAGE-01/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "dep-page-01",
                      "amount": "100.00"
                    }
                '''))
                .andExpect(status().isOk())

        and:
        mockMvc.perform(post("/accounts/PLN-PAGE-01/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "dep-page-02",
                      "amount": "150.00"
                    }
                '''))
                .andExpect(status().isOk())

        expect:
        mockMvc.perform(get("/accounts/PLN-PAGE-01/transactions?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content.length()').value(1))
                .andExpect(jsonPath('$.totalElements').value(3))
                .andExpect(jsonPath('$.totalPages').value(3))
    }
}
