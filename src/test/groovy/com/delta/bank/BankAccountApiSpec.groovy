package com.delta.bank

import com.delta.bank.bootstrap.DeltaApplication
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

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
class BankAccountApiSpec extends BaseIntegrationSpec {

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "creates and reads account"() {
        given:
        def payload = """
            {
              "number": "PLN-1001",
              "owner": "Alice",
              "balance": "1500.00",
              "currency": "PLN"
            }
        """

        when:
        def createResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        createResponse.andExpect(status().isCreated())
                .andExpect(jsonPath('$.number').value('PLN-1001'))
                .andExpect(jsonPath('$.owner').value('Alice'))
                .andExpect(jsonPath('$.balance').value(1500.0))
                .andExpect(jsonPath('$.currency').value('PLN'))

        when:
        def readResponse = mockMvc.perform(get("/accounts/PLN-1001"))

        then:
        readResponse.andExpect(status().isOk())
                .andExpect(jsonPath('$.number').value('PLN-1001'))
                .andExpect(jsonPath('$.owner').value('Alice'))
    }
}