package com.delta.bank

import spock.lang.Specification
import spock.lang.Unroll

class SpockDataTablesSpec extends Specification {

    @Unroll
    def "currency #currency should be valid: #valid"() {
        expect:
        isValidCurrency(currency) == valid

        where:
        currency || valid
        "PLN"    || true
        "EUR"    || true
        "USD"    || true
        "GBP"    || true
        "pln"    || false
        "Euro"   || false
        "EURO"   || false
        ""       || false
        null     || false
    }

    @Unroll
    def "amount #amount should be valid: #valid"() {
        expect:
        isValidAmount(amount) == valid

        where:
        amount               || valid
        new BigDecimal("1")  || true
        new BigDecimal("0.01") || true
        new BigDecimal("100.00") || true
        new BigDecimal("0")  || false
        new BigDecimal("-1") || false
        new BigDecimal("-0.01") || false
        null                 || false
    }

    @Unroll
    def "withdrawal of #withdrawal from balance #balance should be allowed: #allowed"() {
        expect:
        canWithdraw(balance, withdrawal) == allowed

        where:
        balance                | withdrawal              || allowed
        new BigDecimal("100")  | new BigDecimal("50")    || true
        new BigDecimal("100")  | new BigDecimal("100")   || true
        new BigDecimal("100")  | new BigDecimal("0.01")  || true
        new BigDecimal("100")  | new BigDecimal("101")   || false
        new BigDecimal("100")  | new BigDecimal("100.01") || false
        new BigDecimal("0")    | new BigDecimal("1")     || false
        new BigDecimal("100")  | new BigDecimal("0")     || false
        new BigDecimal("100")  | new BigDecimal("-1")    || false
    }

    @Unroll
    def "amount #amount is inside range #minimum to #maximum: #inside"() {
        expect:
        isInsideRange(amount, minimum, maximum) == inside

        where:
        amount               | minimum              | maximum              || inside
        new BigDecimal("100") | new BigDecimal("50")  | new BigDecimal("200") || true
        new BigDecimal("50")  | new BigDecimal("50")  | new BigDecimal("200") || true
        new BigDecimal("200") | new BigDecimal("50")  | new BigDecimal("200") || true
        new BigDecimal("49.99") | new BigDecimal("50") | new BigDecimal("200") || false
        new BigDecimal("200.01") | new BigDecimal("50") | new BigDecimal("200") || false
    }

    private static boolean isValidCurrency(String currency) {
        currency != null && currency ==~ /[A-Z]{3}/
    }

    private static boolean isValidAmount(BigDecimal amount) {
        amount != null && amount.signum() > 0
    }

    private static boolean canWithdraw(BigDecimal balance, BigDecimal withdrawal) {
        balance != null &&
                withdrawal != null &&
                withdrawal.signum() > 0 &&
                balance.compareTo(withdrawal) >= 0
    }

    private static boolean isInsideRange(
            BigDecimal amount,
            BigDecimal minimum,
            BigDecimal maximum
    ) {
        amount != null &&
                minimum != null &&
                maximum != null &&
                amount.compareTo(minimum) >= 0 &&
                amount.compareTo(maximum) <= 0
    }
}