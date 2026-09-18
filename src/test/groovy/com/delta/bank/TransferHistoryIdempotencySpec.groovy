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
class TransferHistoryIdempotencySpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "duplicate transfer request does not duplicate transaction history"() {
        given:
        service.openAccount('PLN-8001', 'Alice', new BigDecimal('1000.00'), 'PLN')
        service.openAccount('PLN-8002', 'Bob', new BigDecimal('250.00'), 'PLN')

        and:
        def payload = """
            {
              "requestId": "req-8001",
              "fromAccount": "PLN-8001",
              "toAccount": "PLN-8002",
              "amount": "150.00"
            }
        """


        /*
        Ten fragment testuje idempotencję transferu.
        Pierwszy POST  /accounts/transfer wysyła transfer z payload.
        Oczekiwany wynik:
        status 200 OK
        $.executed == true
        To znaczy: transfer został faktycznie wykonany.
        * */
        when:
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(true))

        /*
        2. wywołanie = nie wykonuj drugi raz, bo to ten sam request
        .andExpect(jsonPath('$.executed').value(false))
           To właśnie sprawdza, czy mechanizm requestId chroni przed podwójnym zaksięgowaniem tej samej operacji.
        * */
        and:
        mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(false))

        then:
        mockMvc.perform(get("/accounts/PLN-8001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content[?(@.transferRequestId == "req-8001")]')
                        .value(org.hamcrest.Matchers.hasSize(1)))

        and:
        mockMvc.perform(get("/accounts/PLN-8002/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.content').isArray())
                .andExpect(jsonPath('$.content[?(@.transferRequestId == "req-8001")]')
                        .value(org.hamcrest.Matchers.hasSize(1)))

        and:
        mockMvc.perform(get("/accounts/PLN-8001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(850.0))

        and:
        mockMvc.perform(get("/accounts/PLN-8002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(400.0))
    }
}
