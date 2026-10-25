package com.delta.bank.lab.java21.immutability

import spock.lang.Specification

class AccountStatementSpec extends Specification {

    def "opisy wyciągu nie zmieniają się po modyfikacji listy źródłowej"() {

    //  Ten test sprawdza, czy AccountStatement robi kopię listy przy tworzeniu.
    //  Po utworzeniu obiektu `statement` zmieniamy source;  sources.add("Withdrawal: 20.00 PLN")
    //  statement.descriptions() ma nadal zawierać tylko pierwszy opis, a source już oba.
    //  Dzięki temu późniejsze zmiany listy przekazanej do konstruktora nie zmieniają wyciągu.
    //  Nazwa testu mówi właśnie: „opisy wyciągu nie zmieniają się po modyfikacji listy źródłowej”.

        given:
        def sources = new ArrayList<String>()
        sources.add("Deposit: 100.00 PLN")

        def statement = new AccountStatement("PLN-1001", sources)

        when:
        sources.add("Withdrawal: 20.00 PLN")

        then:
        statement.descriptions() == ["Deposit: 100.00 PLN"]
        sources == ["Deposit: 100.00 PLN", "Withdrawal: 20.00 PLN"]
    }

    def "nie pozwala na dodawanie opisów przez akcesor, ponieważ lista jest niemodyfikowalna w typie Record"() {
        given:
        def statement = new AccountStatement(
                "PLN-1001",
                List.of("Deposit: 100.00 PLN")
        )

        when:
        // Test sprawdza, czy lista jest niemodyfikowalna — dodanie powinno rzucić UnsupportedOperationException.
        statement.descriptions().add("Transfer: 10.00 PLN")

        then:
        thrown(UnsupportedOperationException)
    }

    def "nie pozwala na zastępowanie opisu, ponieważ lista jest niemodyfikowalna w typie Record"() {
        given:
        def statement = new AccountStatement(
                "PLN-1001",
                List.of("Deposit: 100.00 PLN")
        )

        when:
        statement.descriptions().set(0, "Changed")

        then:
        thrown(UnsupportedOperationException)
    }

    def "akceptuje pusty wyciąg, ponieważ lista jest niemodyfikowalna w typie Record"() {
        expect:
        new AccountStatement("PLN-1001", List.of()).descriptions().isEmpty()
    }

    def "odrzuca null jako listę opisów"() {
        when:
        new AccountStatement("PLN-1001", null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "descriptions must not be null"
    }

    def "odrzuca null jako opis"() {
        given:
        def descriptions = new ArrayList<String>()
        descriptions.add(null)

        when:
        new AccountStatement("PLN-1001", descriptions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "description must not be null"
    }

    def "odrzuca pusty numer konta"() {
        when:
        new AccountStatement(" \t ", List.of())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }

    def "odrzuca null jako numer konta"() {
        when:
        new AccountStatement(null, List.of())

        then:
        def exception = thrown(NullPointerException)
        exception.message == "accountNumber must not be null"
    }
}