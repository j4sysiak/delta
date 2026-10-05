package com.delta.bank.lab.java21.text

import spock.lang.Specification
import spock.lang.Unroll

class AccountReportFormatterSpec extends Specification {

    def "formats an account report with exact line breaks"() {
        given:
        def formatter = new AccountReportFormatter()

        when:
        def report = formatter.format("PLN-1001", new BigDecimal("1000.00"))

        then:
        report == "Account report\nAccount: PLN-1001\nBalance: 1000.00 PLN\n"
    }

    @Unroll
    def "preserves balance representation for #amount"() {
        given:
        def formatter = new AccountReportFormatter()

        when:
        def report = formatter.format("PLN-1002", new BigDecimal(amount))

        then:
        report == "Account report\nAccount: PLN-1002\nBalance: ${expected} PLN\n"

        where:
        amount    | expected
        "150.00"  | "150.00"
        "0.00"    | "0.00"
        "1000"    | "1000"
        "1E+3"    | "1000"
    }

    // @Unroll w Spocku rozwija test parametryzowany z bloku where: na osobne przypadki testowe.
    // zostanie uruchomiony osobno dla każdej wartości z tabeli where:
    //     zamiast jako jeden zbiorczy test.
    // Dzięki temu w raporcie widać dokładnie, który przypadek przeszedł albo padł,
    // np. dla amount = "150.00" albo amount = "1E+3".
    // Dodatkowo placeholdery w nazwie testu, takie jak #amount albo #argument,
    // są podstawiane konkretną wartością dla danego uruchomienia.
    @Unroll
    def "rejects null #argument"() {
        given:
        def formatter = new AccountReportFormatter()

        when:
        formatter.format(accountNumber, balance)

        then:
        def exception = thrown(NullPointerException)
        exception.message == message

        where:
        argument        | accountNumber | balance                 | message
        "accountNumber" | null          | new BigDecimal("10.00") | "accountNumber must not be null"
        "balance"       | "PLN-1001"    | null                    | "balance must not be null"
    }
}