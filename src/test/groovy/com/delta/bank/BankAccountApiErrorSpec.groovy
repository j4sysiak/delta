package com.delta.bank

import com.delta.bank.application.service.BankAccountService
import com.delta.bank.bootstrap.DeltaApplication
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = DeltaApplication)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankAccountApiErrorSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    MockMvc mockMvc

    def setup() {
        // Inicjalizacja MockMvc z kontekstu Spring dla testów endpointów HTTP.
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "returns 400 for invalid currency"() {
        given:
        def payload = """
            {
              "number": "PLN-7001",
              "owner": "Alice",
              "balance": "1000",
              "currency": "pln"
            }
        """

        when:
        def response = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        response.andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.reason').value('Bad Request'))
                .andExpect(jsonPath('$.errorCode').value('VALIDATION_ERROR'))
                .andExpect(jsonPath('$.message').value('Currency must be a 3-letter uppercase code'))
    }

    def "returns 409 for insufficient funds on withdraw"() {
        given:
        service.openAccount('PLN-7002', 'Alice', new BigDecimal('100.00'), 'PLN')

        and:
        def payload = """
            {
              "requestId": "wd-7002",
              "amount": "200.00"
            }
        """

        when:
        def response = mockMvc.perform(post("/accounts/PLN-7002/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        response.andExpect(status().isConflict())
                .andExpect(jsonPath('$.status').value(409))
                .andExpect(jsonPath('$.reason').value('Conflict'))
                .andExpect(jsonPath('$.errorCode').value('INSUFFICIENT_FUNDS'))
                .andExpect(jsonPath('$.message').value('Insufficient funds'))
    }


    /*
    Tworzone są dwa konta:
    1. PLN-7003 z saldem 1000.00
    2. PLN-7004 z saldem 250.00

    Budowany jest JSON przelewu:
     requestId = req-7003
     przelew 150.00 z PLN-7003 do PLN-7004

     Pierwsze wywołanie POST /accounts/transfer:
      - oczekiwane 200 OK
      - executed = true
      - requestId = req-7003

      To znaczy: przelew został wykonany.


        Drugie wywołanie POST /accounts/transfer z tym samym payload:
        - oczekiwane 200 OK
        - executed = false
        - requestId = req-7003

        To znaczy: przelew nie został wykonany ponownie, bo requestId już istnieje.
        To znaczy: system rozpoznał duplikat i nie wykonał przelewu drugi raz.

        Na końcu wykonywana jest wypłata 0.01 z konta PLN-7003 i oczekiwane jest 200 OK.
        Sens ostatniego kroku:
        - po pierwszym przelewie saldo powinno spaść z 1000.00 do 850.00
        - jeśli drugi przelew zostałby wykonany błędnie drugi raz, saldo spadłoby do 700.00
          (Jeśli ten sam przelew zostałby wykonany błędnie drugi raz, system znów odjąłby 150.00:  850.00 - 150.00 = 700.00)
        - ten test tylko pośrednio sprawdza, że konto nadal jest w poprawnym stanie
    */
    def "returns 200 for duplicate transfer request id without double debit"() {
        given:
        service.openAccount('PLN-7003', 'Alice', new BigDecimal('1000.00'), 'PLN')
        service.openAccount('PLN-7004', 'Bob', new BigDecimal('250.00'), 'PLN')

        and:
        def payload = """
            {
              "requestId": "req-7003",
              "fromAccount": "PLN-7003",
              "toAccount": "PLN-7004",
              "amount": "150.00"
            }
        """

        when:
        def first = mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        first.andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(true))
                .andExpect(jsonPath('$.requestId').value('req-7003'))
                .andExpect(jsonPath('$.accountNumber').value('PLN-7003'))

        when:
        def second = mockMvc.perform(post("/accounts/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))

        then:
        second.andExpect(status().isOk())
                .andExpect(jsonPath('$.executed').value(false))
                .andExpect(jsonPath('$.requestId').value('req-7003'))
                .andExpect(jsonPath('$.accountNumber').value('PLN-7003'))

        and:
        mockMvc.perform(get("/accounts/PLN-7003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(850.0))

        and:
        mockMvc.perform(get("/accounts/PLN-7004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.balance').value(400.0))
    }
}