package com.delta.bank

import com.delta.bank.bootstrap.DeltaApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DeltaApplication
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankAccountValidationSpec extends BaseIntegrationSpec {

    @Autowired
    WebApplicationContext context

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "rejects invalid currency"() {
        given:
        def payload = """
            {
              "number": "PLN-2001",
              "owner": "Alice",
              "balance": "1000",
              "currency": "xxx"
            }
        """

        when:
        def response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.error').value('Bad Request'))
    }

    def "rejects negative balance"() {
        given:
        def payload = """
            {
              "number": "PLN-2002",
              "owner": "Alice",
              "balance": "-10",
              "currency": "PLN"
            }
        """

        when:
        def response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        response.andExpect(status().isBadRequest())
    }
}