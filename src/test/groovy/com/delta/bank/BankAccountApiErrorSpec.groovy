import com.delta.bank.BaseIntegrationSpec
import com.delta.bank.application.BankAccountService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.context.WebApplicationContext
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.http.MediaType
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BankAccountApiErrorSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    MockMvc mockMvc

    def setup() {
        WebApplicationContext context = applicationContext
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    def "returns 409 for insufficient funds on withdraw"() {
        given:
        service.openAccount('PLN-7002', 'Alice', new BigDecimal('100.00'), 'PLN')

        and:
        def payload = """
            {
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
                .andExpect(jsonPath('$.error').value('Conflict'))
    }
}