package com.delta.bank.lab.java21.strings

import spock.lang.Specification
import spock.lang.Unroll

class AccountStatementFormatterSpec extends Specification {

    private final AccountStatementFormatter formatter =
            new AccountStatementFormatter()

    def "strips surrounding whitespace and formats an account statement"() {
        when:
        def statement = formatter.format(
                " \t PLN-1001 \n",
                "  salary payment  ",
                new BigDecimal("1250.00")
        )

        then:
        statement == "Account statement\n" +
                "Account: PLN-1001\n" +
                "Description: salary payment\n" +
                "Balance: 1250.00 PLN\n" +
                "------------------------\n"
    }

    def "splits statement into lines without a trailing empty line"() {
        given:
        def statement = formatter.format(
                "PLN-1001",
                "    salary payment",
                new BigDecimal("1250.00")
        )

        expect:
        formatter.lines(statement) == [
                "Account statement",
                "Account: PLN-1001",
                "Description: salary payment",
                "Balance: 1250.00 PLN",
                "------------------------"
        ]
    }

    def "String lines returns no lines for an empty string"() {
        expect:
        " ".lines().toList().isEmpty()
    }

    def "String lines recognizes LF CRLF and CR separators"() {
        expect:
        "one\ntwo\r\nthree\rfour".lines().toList() ==
                ["one", "two", "three", "four"]
    }

    @Unroll
    def "rejects blank #field"() {
        when:
        formatter.format(accountNumber, description, new BigDecimal("10.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == message

        where:
        field           | accountNumber | description | message
        "account number"| " \t\n"       | "payment"   | "accountNumber must not be blank"
        "description"   | "PLN-1001"    | " \t\n"     | "description must not be blank"
    }

    @Unroll
    def "rejects null #field"() {
        when:
        formatter.format(accountNumber, description, balance)

        then:
        def exception = thrown(NullPointerException)
        exception.message == message

        where:
        field            | accountNumber | description | balance                 | message
        "account number" | null          | "payment"   | new BigDecimal("10.00") | "accountNumber must not be null"
        "description"    | "PLN-1001"    | null        | new BigDecimal("10.00") | "description must not be null"
        "balance"        | "PLN-1001"    | "payment"   | null                    | "balance must not be null"
    }
}