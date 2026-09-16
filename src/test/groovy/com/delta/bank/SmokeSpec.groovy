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
class SmokeSpec extends BaseIntegrationSpec {

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "health is up"() {
        expect:
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value('UP'))
                .andExpect(jsonPath('$.service').value('delta-mini-bank'))
    }

    def "readiness and liveness are ok"() {
        expect:
        mockMvc.perform(get("/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value('READY'))
                .andExpect(jsonPath('$.service').value('delta-mini-bank'))

        and:
        mockMvc.perform(get("/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.status').value('ALIVE'))
                .andExpect(jsonPath('$.service').value('delta-mini-bank'))
    }

    def "swagger docs are exposed"() {
        expect:
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.openapi').exists())

        and:
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
    }

    def "create account works"() {
        given:
        def payload = '''
            {
              "number": "PLN-SMOKE-01",
              "owner": "Alice",
              "balance": "1500.00",
              "currency": "PLN"
            }
        '''

        expect:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.number').value('PLN-SMOKE-01'))
                .andExpect(jsonPath('$.owner').value('Alice'))
                .andExpect(jsonPath('$.currency').value('PLN'))
                .andExpect(jsonPath('$.balance').value(1500.0))
    }

    def "deposit works"() {
        given:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-SMOKE-02",
                      "owner": "Alice",
                      "balance": "1000.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        def payload = '''
            {
              "requestId": "dep-smoke-02",
              "amount": "250.00"
            }
        '''

        expect:
        mockMvc.perform(post("/accounts/PLN-SMOKE-02/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.requestId').value('dep-smoke-02'))
                .andExpect(jsonPath('$.accountNumber').value('PLN-SMOKE-02'))
                .andExpect(jsonPath('$.balance').value(1250.0))
    }

    def "transfer works"() {
        given:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-SMOKE-03",
                      "owner": "Alice",
                      "balance": "1000.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-SMOKE-04",
                      "owner": "Bob",
                      "balance": "250.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        def payload = '''
            {
              "requestId": "tr-smoke-01",
              "fromAccount": "PLN-SMOKE-03",
              "toAccount": "PLN-SMOKE-04",
              "amount": "150.00"
            }
        '''

        expect:
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(true))
                .andExpect(jsonPath('$.requestId').value('tr-smoke-01'))
                .andExpect(jsonPath('$.amount').value(150.0))

        and:
        mockMvc.perform(get("/accounts/PLN-SMOKE-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(850.0))

        and:
        mockMvc.perform(get("/accounts/PLN-SMOKE-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(400.0))
    }

    def "history is readable"() {
        given:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "number": "PLN-SMOKE-05",
                      "owner": "Alice",
                      "balance": "1000.00",
                      "currency": "PLN"
                    }
                '''))
                .andExpect(status().isCreated())

        and:
        mockMvc.perform(post("/accounts/PLN-SMOKE-05/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content('''
                    {
                      "requestId": "hist-smoke-01",
                      "amount": "200.00"
                    }
                '''))
                .andExpect(status().isOk())

        expect:
        mockMvc.perform(get("/accounts/PLN-SMOKE-05/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content[0].accountNumber').value('PLN-SMOKE-05'))
                .andExpect(jsonPath('$.content[0].type').value('DEPOSIT'))
                .andExpect(jsonPath('$.content[0].transferRequestId').value('hist-smoke-01'))
    }
}