package com.delta.bank.lab.java21.virtualthreads

import spock.lang.Specification

import java.util.concurrent.Callable

class BankCallLimiterSpec extends Specification {

    def "returns operation result"() {
        given:
        def limiter = new BankCallLimiter(2)

        when:
        def result = limiter.execute({
            "statement loaded"
        } as Callable<String>)

        then:
        result == "statement loaded"
    }

    def "releases permit when operation throws"() {
        given:
        def limiter = new BankCallLimiter(1)

        when:
        limiter.execute({
            throw new IllegalStateException("remote service failed")
        } as Callable<String>)

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "remote service failed"

        when:
        def result = limiter.execute(
                { "next call succeeded" } as Callable<String>
        )

        then:
        result == "next call succeeded"
    }

    def "rejects a non-positive limit"() {
        when:
        new BankCallLimiter(0)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "maxConcurrentCalls must be positive"
    }

    def "rejects a null operation"() {
        given:
        def limiter = new BankCallLimiter(1)

        when:
        limiter.execute(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "operation must not be null"
    }
}