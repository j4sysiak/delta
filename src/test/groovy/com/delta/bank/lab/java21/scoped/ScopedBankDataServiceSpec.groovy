package com.delta.bank.lab.java21.scoped

import spock.lang.Specification

class ScopedBankDataServiceSpec extends Specification {

    def "passes request context to structured child tasks"() {
        given:
        def loader = loaderReadingContext()
        def service = new ScopedBankDataService(loader)
        def context = new RequestContext("request-1001", "anna")

        when:
        def result = service.loadAll("PLN-1001", context)

        then:
        result == new BankDataResult(
                "request-1001:anna:PLN-1001:balance",
                "request-1001:anna:PLN-1001:history",
                "request-1001:anna:PLN-1001:summary"
        )
        // Ten warunek sprawdza, że po zakończeniu wywołania żaden RequestContext nie pozostał przypięty do bieżącego wątku.
        !RequestContextHolder.isBound()
    }

    def "isolates context between consecutive requests"() {
        given:
        def loader = loaderReadingContext()
        def service = new ScopedBankDataService(loader)

        when:
        def firstResult = service.loadAll(
                "PLN-2001",
                new RequestContext("request-first", "anna")
        )
        def secondResult = service.loadAll(
                "PLN-2002",
                new RequestContext("request-second", "jan")
        )

        then:
        firstResult.balance() == "request-first:anna:PLN-2001:balance"
        secondResult.balance() == "request-second:jan:PLN-2002:balance"
        !RequestContextHolder.isBound()
    }

    def "propagates failure from a child task"() {
        given:
        def loader = Stub(BankDataLoader) {
            loadBalance("PLN-3001") >> {
                assert RequestContextHolder.current() ==
                        new RequestContext("request-3001", "ola")
                throw new IllegalStateException("Balance service unavailable")
            }
            loadHistory("PLN-3001") >> "HISTORY: 2 transactions"
            loadSummary("PLN-3001") >> "SUMMARY: +50.00 PLN"
        }
        def service = new ScopedBankDataService(loader)

        when:
        service.loadAll(
                "PLN-3001",
                new RequestContext("request-3001", "ola")
        )

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Bank data loading failed"
        exception.cause.message == "Balance service unavailable"
        !RequestContextHolder.isBound()
    }

    def "rejects null account number"() {
        given:
        def service = new ScopedBankDataService(Stub(BankDataLoader))

        when:
        service.loadAll(
                null,
                new RequestContext("request-4001", "ewa")
        )

        then:
        def exception = thrown(NullPointerException)
        exception.message == "accountNumber must not be null"
    }

    private static BankDataLoader loaderReadingContext() {

        return new BankDataLoader() {
            @Override
            String loadBalance(String accountNumber) {
                def context = RequestContextHolder.current()
                return "${context.correlationId()}:${context.username()}:${accountNumber}:balance"
            }

            @Override
            String loadHistory(String accountNumber) {
                def context = RequestContextHolder.current()
                return "${context.correlationId()}:${context.username()}:${accountNumber}:history"
            }

            @Override
            String loadSummary(String accountNumber) {
                def context = RequestContextHolder.current()
                return "${context.correlationId()}:${context.username()}:${accountNumber}:summary"
            }
        }
    }

}