package com.delta.bank.lab.java21.resources

import spock.lang.Specification

class StatementReaderSpec extends Specification {

    def "new session is open"() {
        given:
        def session = new StatementSession()

        expect:
        !session.isClosed()

        cleanup:
        session.close()
    }

    def "zamyka sesję przed zwróceniem wyciągu (Statment)"() {
        given:
        def session = new StatementSession()
        def reader = new StatementReader()

        when:
        def statement = reader.readAndClose(session)

        then:
        statement == "Statement for PLN-1001"
        session.isClosed()

        cleanup:
        session.close()
    }

    def "closes session even when processing fails"() {
        given:
        def session = new StatementSession()
        def reader = new StatementReader()

        when:
        reader.failAndClose(session)

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Statement processing failed"
        session.isClosed()

        cleanup:
        session.close()
    }

    def "cannot read a closed session"() {
        given:
        def session = new StatementSession()
        session.close()

        when:
        session.readStatement()

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Session is closed"
    }
}