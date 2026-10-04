package com.delta.bank.lab.java21.structured

import spock.lang.Specification

class StructuredBankDataServiceSpec extends Specification {

    def "loads balance history and summary as one structured operation"() {
        given:
        def loader = Stub(BankDataLoader) {
            loadBalance("PLN-1001") >> "BALANCE: 1000.00 PLN"
            loadHistory("PLN-1001") >> "HISTORY: 3 transactions"
            loadSummary("PLN-1001") >> "SUMMARY: +150.00 PLN"
        }
        def service = new StructuredBankDataService(loader)

        when:
        def result = service.loadAll("PLN-1001")

        then:
        result == new BankDataResult(
                "BALANCE: 1000.00 PLN",
                "HISTORY: 3 transactions",
                "SUMMARY: +150.00 PLN"
        )
    }

    def "propagates failure from a child task"() {
        given:
        def loader = Stub(BankDataLoader) {
            loadBalance("PLN-1002") >> {
                throw new IllegalStateException("Balance service unavailable")
            }
            loadHistory("PLN-1002") >> "HISTORY: 2 transactions"
            loadSummary("PLN-1002") >> "SUMMARY: +50.00 PLN"
        }
        def service = new StructuredBankDataService(loader)

        when:
        service.loadAll("PLN-1002")

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Bank data loading failed"
        exception.cause.message == "Balance service unavailable"
    }

    def "rejects null account number"() {
        given:
        def loader = Stub(BankDataLoader)
        def service = new StructuredBankDataService(loader)

        when:
        service.loadAll(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "accountNumber must not be null"
    }
}