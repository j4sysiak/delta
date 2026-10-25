# Lab 23 — Rekordy i niemutowalność: kopia defensywna

## Cel

Rekord ma pola `final`, ale to nie znaczy, że wszystko, co przechowuje,
jest niemutowalne.

Przećwiczymy jedną zasadę:

> Jeśli rekord przyjmuje listę, zastanów się, czy powinien zachować
> niezależną, niemodyfikowalną kopię jej struktury.

Użyjemy `List.copyOf()` dostępnego od Java 10 i rekordów finalnych
od Java 16. Całość działa na Java 21 bez preview, Springa, bazy i wątków.
Nie zmieniaj konfiguracji Gradle ani produkcyjnego mini-banku.

## 1. Problem: `final` chroni referencję, a nie zawartość listy

Taki rekord nie zabezpiecza listy:

```java
public record StatementDraft(List<String> descriptions) {
}
```

Nie można przypisać innej listy do pola rekordu, ale można zmieniać listę,
na którą to pole wskazuje:

```java
var source = new ArrayList<String>();
source.add("Deposit");

var draft = new StatementDraft(source);
source.add("Withdrawal");
```

Teraz `draft.descriptions()` zawiera dwa elementy. Rekord przechowuje
referencję do tej samej listy co `source`.

Również to zmieni jego zawartość:

```java
draft.descriptions().add("Transfer");
```

To nie błąd rekordu. Rekord zapewnia płytką niemutowalność swoich pól,
nie automatyczną głęboką niemutowalność całego grafu obiektów.

## 2. Rozwiązanie: `List.copyOf()`

```java
public record AccountStatement(List<String> descriptions) {
    public AccountStatement {
        descriptions = List.copyOf(descriptions);
    }
}
```

`List.copyOf()` zapewnia dwa istotne zachowania:

- późniejsze zmiany struktury źródłowej listy nie zmieniają wyniku,
- wynikowa lista nie pozwala dodawać, usuwać ani zastępować elementów.

Nie musisz ręcznie tworzyć nowej listy w każdym wywołaniu accessora.

Uwaga: metoda nie gwarantuje utworzenia nowego obiektu listy w każdym
przypadku. Może wykorzystać istniejącą listę, jeżeli spełnia wymagania.
Ważny jest kontrakt wyniku, nie tożsamość obiektu.

## 3. Klasa do wklejenia

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\immutability\AccountStatement.java
```

Wklej:

```java
package com.delta.bank.lab.java21.immutability;

import java.util.List;
import java.util.Objects;

public record AccountStatement(
        String accountNumber,
        List<String> descriptions
) {

    public AccountStatement {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(descriptions, "descriptions must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }

        for (var description : descriptions) {
            Objects.requireNonNull(description, "description must not be null");
        }

        descriptions = List.copyOf(descriptions);
    }
}
```

To konstruktor kompaktowy rekordu. Instrukcja:

```java
descriptions = List.copyOf(descriptions);
```

zmienia parametr konstruktora. Po wykonaniu ciała konstruktora Java
automatycznie przypisze go do pola rekordu.

Nie pisz tutaj `this.descriptions = ...`: w konstruktorze kompaktowym
przypisanie do pola wykonuje kompilator.

Pusta lista jest dozwolona. `null` jako lista lub jej element jest
odrzucany jawnie. Samo `List.copyOf()` także odrzuca `null`, ale własne
sprawdzenia pozwalają określić czytelny komunikat.

## 4. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\immutability\AccountStatementSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.immutability

import spock.lang.Specification

class AccountStatementSpec extends Specification {

    def "keeps descriptions unchanged when the source list changes"() {
        given:
        def source = new ArrayList<String>()
        source.add("Deposit: 100.00 PLN")
        def statement = new AccountStatement("PLN-1001", source)

        when:
        source.add("Withdrawal: 20.00 PLN")

        then:
        statement.descriptions() == ["Deposit: 100.00 PLN"]
        source == ["Deposit: 100.00 PLN", "Withdrawal: 20.00 PLN"]
    }

    def "does not allow adding descriptions through the accessor"() {
        given:
        def statement = new AccountStatement(
                "PLN-1001",
                List.of("Deposit: 100.00 PLN")
        )

        when:
        statement.descriptions().add("Transfer: 10.00 PLN")

        then:
        thrown(UnsupportedOperationException)
    }

    def "does not allow replacing a description"() {
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

    def "accepts an empty statement"() {
        expect:
        new AccountStatement("PLN-1001", List.of()).descriptions().isEmpty()
    }

    def "rejects a null descriptions list"() {
        when:
        new AccountStatement("PLN-1001", null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "descriptions must not be null"
    }

    def "rejects a null description"() {
        given:
        def descriptions = new ArrayList<String>()
        descriptions.add(null)

        when:
        new AccountStatement("PLN-1001", descriptions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "description must not be null"
    }

    def "rejects a blank account number"() {
        when:
        new AccountStatement(" \t ", List.of())

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }

    def "rejects a null account number"() {
        when:
        new AccountStatement(null, List.of())

        then:
        def exception = thrown(NullPointerException)
        exception.message == "accountNumber must not be null"
    }
}
```

Pierwszy test sprawdza ochronę przed zmianą wejścia. Drugi i trzeci
sprawdzają ochronę przed zmianą przez accessor. To dwa różne miejsca,
przez które lista mogłaby zostać zmodyfikowana.

## 5. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.immutability.AccountStatementSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.immutability.AccountStatementSpec"
```

## 6. Eksperyment: usuń kopię

Na chwilę usuń z konstruktora:

```java
descriptions = List.copyOf(descriptions);
```

Uruchom testy.

Pierwszy test nie przejdzie: modyfikacja źródłowego `ArrayList` zmieni
również listę przechowywaną w rekordzie.

Testy dodawania i zastępowania nadal mogą przejść, bo w nich wejściem jest
już niemodyfikowalne `List.of(...)`. Dlatego sprawdzenie tylko wyjątku
z `add()` nie wystarcza do przetestowania kopii defensywnej.

Przywróć `List.copyOf()`.

## 7. Ważne: to nie jest głęboka kopia elementów

`List.copyOf()` kopiuje strukturę kolekcji, nie klonuje elementów.

W tym labie elementami są `String`, które są niemutowalne. Dlatego nie
można zmienić ich zawartości po utworzeniu rekordu.

Gdyby lista zawierała mutowalne obiekty, nadal można byłoby zmieniać te
obiekty, mimo że nie można wymienić ich w liście. Wtedy trzeba osobno
zaprojektować niemutowalność lub kopiowanie elementów.

Nie zastępuj bez zastanowienia kopii przez:

```java
Collections.unmodifiableList(source)
```

Ta metoda tworzy niemodyfikowalny widok. Zmiana oryginalnego `source`
nadal byłaby widoczna przez ten widok.

## Pytania kontrolne

1. Dlaczego samo pole `final List<String>` nie zabezpiecza listy?
2. Przed jakimi dwoma sposobami zmiany chroni nasza implementacja?
3. Czy `List.copyOf()` tworzy głęboką kopię elementów?
4. Dlaczego `String` jest wygodnym elementem niemutowalnego modelu?
5. Czym kopia różni się od `Collections.unmodifiableList(source)`?
6. Czy konstruktor kompaktowy sam przypisuje parametry do pól?

Do zapamiętania:

```text
record → finalne referencje, nie automatycznie niemutowalne elementy
List.copyOf → niezależna struktura listy bez możliwości modyfikacji
niemutowalny model → zadbaj także o typy przechowywanych elementów
```
