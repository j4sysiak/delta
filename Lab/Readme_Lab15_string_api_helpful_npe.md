# Lab 15 — String API i pomocne NullPointerException

## Cel laba

W tym labie przypomnisz sobie przydatne metody `String` dodane w Java 11
oraz diagnostykę `NullPointerException`, ulepszoną w Java 14.

| Funkcja | Wersja |
|---|---|
| `isBlank()` | Java 11 |
| `strip()`, `stripLeading()`, `stripTrailing()` | Java 11 |
| `lines()` | Java 11 |
| `repeat(int)` | Java 11 |
| pomocne komunikaty NPE | Java 14, finalne zachowanie w nowszych JDK |

Kod uruchamiamy na Java 21. Żadna z tych funkcji nie wymaga preview.
Nie zmieniaj konfiguracji Gradle, ponieważ wcześniejsze laby używają
preview API.

Ćwiczenie będzie odizolowane: nie zmieniamy produkcyjnego mini-banku,
nie uruchamiamy Springa ani bazy danych.

## 1. `isBlank()` a `isEmpty()`

```java
"".isEmpty()       // true
"   ".isEmpty()    // false
"   ".isBlank()    // true
```

- `isEmpty()` sprawdza, czy długość napisu wynosi zero.
- `isBlank()` sprawdza, czy napis jest pusty albo składa się wyłącznie
  ze znaków uznawanych przez `Character.isWhitespace` za białe.

To przydatne np. przy walidacji tekstowego numeru rachunku. Nie oznacza
jednak, że `isBlank()` waliduje cały format ani poprawność rachunku.

## 2. `strip()` zamiast bezrefleksyjnego `trim()`

```java
var accountNumber = " \t PLN-1001 \n".strip();
```

`strip()` usuwa białe znaki z obu końców, zgodnie z regułą
`Character.isWhitespace`. Są też metody `stripLeading()` i
`stripTrailing()`.

`trim()` jest starsze i stosuje inną, węższą regułę opartą o wartości
znaków nie większe niż U+0020. Do nowego kodu zwykle wybieraj metodę
odpowiadającą intencji: `strip()` dla białych znaków rozpoznawanych przez
Java, `trim()` tylko wtedy, gdy właśnie jego historyczna reguła jest
pożądana.

Żadna z tych metod nie usuwa spacji ze środka napisu.

## 3. `lines()`

```java
var lines = report.lines().toList();
```

`lines()` zwraca `Stream<String>` z wierszami tekstu. Rozpoznaje separatory
LF (`\n`), CRLF (`\r\n`) oraz CR (`\r`).

Ważna reguła: końcowy separator wiersza nie tworzy dodatkowego pustego
wiersza:

```java
"first\nsecond\n".lines().toList()
// ["first", "second"]
```

Pusty napis daje pusty strumień wierszy:

```java
"".lines().count() // 0
```

`lines()` nie zachowuje separatorów w wynikowych elementach.

## 4. `repeat(int)`

```java
var separator = "-".repeat(20);
```

`repeat(n)` zwraca nowy napis zawierający powtórzenie oryginalnej treści
`n` razy.

- `repeat(0)` zwraca pusty napis,
- wartość ujemna powoduje `IllegalArgumentException`,
- metoda nie zmienia oryginalnego `String`.

## 5. Mini-bankowy formatter

Utworzymy formatter tekstowego raportu. Wejściowy opis normalizujemy
przez `strip()`, puste opisy odrzucamy przez `isBlank()`, raport dzielimy
na wiersze przez `lines()`, a separator powtarzamy przez `repeat()`.

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\strings\AccountStatementFormatter.java
```

Wklej:

```java
package com.delta.bank.lab.java21.strings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class AccountStatementFormatter {

    public String format(
            String accountNumber,
            String description,
            BigDecimal balance
    ) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(balance, "balance must not be null");

        var normalizedAccountNumber = accountNumber.strip();
        if (normalizedAccountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }

        var normalizedDescription = description.strip();
        if (normalizedDescription.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        var separator = "-".repeat(24);
        return """
                Account statement
                Account: %s
                Description: %s
                Balance: %s PLN
                %s
                """.formatted(
                normalizedAccountNumber,
                normalizedDescription,
                balance.toPlainString(),
                separator
        );
    }

    public List<String> lines(String statement) {
        Objects.requireNonNull(statement, "statement must not be null");

        return statement.lines().toList();
    }
}
```

Metoda `format` odrzuca `null` jawnie, a puste i składające się tylko
z białych znaków pola odrzuca po `strip()` i `isBlank()`.

`lines(...)` zwraca niemodyfikowalną listę, ponieważ używa
`Stream.toList()` z Java 16.

## 6. Testy Spocka

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\strings\AccountStatementFormatterSpec.groovy
```

Wklej:

```groovy
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
                "salary payment",
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
        "".lines().toList().isEmpty()
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
```

Zwróć uwagę na zapis w teście `"\n"` i `"\r\n"`: to escape'y Groovy
oznaczające rzeczywiste znaki końca linii.

## 7. Pomocne komunikaty NPE

Przed Java 14 `NullPointerException` często nie mówił, które wyrażenie
było `null`. Nowszy JVM potrafi wskazać, czego program próbował użyć.

Przykład demonstracyjny:

```java
String accountNumber = null;
int length = accountNumber.strip().length();
```

Na Java 21 komunikat może wyglądać podobnie do:

```text
Cannot invoke "String.strip()" because "accountNumber" is null
```

**Pełna treść komunikatu zależy od JDK, kompilatora i opcji uruchomienia.**
Nie testuj dokładnego tekstu jako kontraktu aplikacji.

W JDK, które obsługuje helpful NPE, szczegóły komunikatu można wyłączyć
opcją JVM:

```text
-XX:-ShowCodeDetailsInExceptionMessages
```

To jest diagnostyka, nie walidacja. Nie zastępuje jawnych kontroli
argumentów ani dobrego komunikatu domenowego. W klasie formatowania
powyżej `Objects.requireNonNull` daje stabilny, celowy komunikat.

## 8. Bezpieczna demonstracja NPE

Do zabawy w tym labie dodaj osobną metodę demonstracyjną do klasy
`AccountStatementFormatter`:

```java
public int unsafeLengthForNpeDemo(String value) {
    return value.strip().length();
}
```

Ta metoda celowo nie sprawdza `null`, aby JVM mogła pokazać, na jakim
wywołaniu wyjątek powstał. **Nie kopiuj jej wzorca do kodu produkcyjnego.**

Dodaj do speca:

```groovy
def "JVM provides a helpful diagnostic for an unexpected null"() {
    when:
    formatter.unsafeLengthForNpeDemo(null)

    then:
    def exception = thrown(NullPointerException)
    exception.message != null
    exception.message.toLowerCase().contains("null")
}
```

Test sprawdza jedynie, że komunikat zawiera informację diagnostyczną
o `null`; nie porównuje całego tekstu, który może różnić się między
środowiskami. Jeśli uruchomisz JVM z `-XX:-ShowCodeDetailsInExceptionMessages`,
ten test może nie przejść — wówczas usuń demonstracyjny test lub uruchom
go z domyślnymi opcjami JDK 21.

Po ćwiczeniu możesz usunąć metodę demonstracyjną i jej test.

## 9. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.strings.AccountStatementFormatterSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.strings.AccountStatementFormatterSpec"
```

Ten spec nie wymaga Springa ani bazy danych.

## 10. Krótkie eksperymenty

### `isBlank()` kontra `isEmpty()`

W JShell albo tymczasowym teście sprawdź:

```java
"".isEmpty()
" ".isEmpty()
" ".isBlank()
```

Wyjaśnij, dlaczego drugi i trzeci wynik są różne.

### `strip()` nie usuwa środka

```java
" PLN - 1001 ".strip()
```

Wynik zachowuje spacje wewnątrz tekstu. `strip()` przycina tylko początek
i koniec.

### `repeat()`

Sprawdź:

```java
"*".repeat(0)
"*".repeat(3)
"*".repeat(-1)
```

Ostatnie wywołanie kończy się `IllegalArgumentException`.

## Pytania kontrolne

1. Czym `isBlank()` różni się od `isEmpty()`?
2. Jaką regułę białych znaków stosuje `strip()`?
3. Czy `lines()` zwraca końcowy pusty element dla tekstu zakończonego
   separatorem?
4. Czy `repeat(0)` zwraca `null`?
5. Czy dokładny helpful NPE message jest stabilnym API?
6. Czy diagnostyka NPE zastępuje walidację wejścia?

Do zapamiętania:

```text
isEmpty → długość równa zero
isBlank → pusty lub tylko whitespace
strip   → przycięcie whitespace z końców
lines   → strumień wierszy
repeat  → powtórzenie napisu
helpful NPE → diagnostyka JVM, nie kontrakt aplikacji
```
