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
class BankAccountApiSpec extends BaseIntegrationSpec {

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    /*
    Utworzenie i odczyt konta - wysyła POST /accounts z danymi konta
    - sprawdza, czy API zwraca 201 Created
    - potem robi GET /accounts/PLN-1001
    - sprawdza, czy konto da się odczytać i dane są poprawne
    * */
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


    /*
    Przelew między kontami - tworzy dwa konta
     - wysyła POST /accounts/transfer z requestId
     - sprawdza, czy przelew został wykonany
     - potem odczytuje oba konta i weryfikuje nowe salda
    * */
    def "transfer executes with request id"() {
        given:
        def createAlice = """
            {
              "number": "PLN-3001",
              "owner": "Alice",
              "balance": "1000.00",
              "currency": "PLN"
            }
        """
        def createBob = """
            {
              "number": "PLN-3002",
              "owner": "Bob",
              "balance": "250.00",
              "currency": "PLN"
            }
        """
        def transferPayload = """
            {
              "requestId": "req-3001",
              "fromAccount": "PLN-3001",
              "toAccount": "PLN-3002",
              "amount": "150.00"
            }
        """

        and:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createAlice))
                .andExpect(status().isCreated())

        and:
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBob))
                .andExpect(status().isCreated())

        when:
        def transferResponse = mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferPayload))

        then:
        transferResponse.andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(true))
                .andExpect(jsonPath('$.requestId').value('req-3001'))
                .andExpect(jsonPath('$.accountNumber').value('PLN-3001'))
                .andExpect(jsonPath('$.amount').value(150.0))

        and:
        mockMvc.perform(get("/accounts/PLN-3001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(850.0))

        and:
        mockMvc.perform(get("/accounts/PLN-3002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(400.0))
    }
}