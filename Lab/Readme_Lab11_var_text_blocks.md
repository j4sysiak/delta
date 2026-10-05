# Lab 11 — `var` i text blocks

## Cel laba

Po Lab 10 robimy prostsze ćwiczenie: bez wątków, Springa i API preview.
Tworzymy tekstowy raport konta i testujemy jego dokładną treść w Spocku.

Poznasz dwa elementy dostępne także w Javie 17:

| Element | Od której wersji jest finalny? |
|---|---|
| `var` dla zmiennych lokalnych | Java 10 |
| Text blocks, czyli wielowierszowe napisy `"""` | Java 15 |

Cały kod piszemy i uruchamiamy na JDK 21. Te funkcje nie wymagają preview.
Nie zmieniaj istniejącej konfiguracji Gradle: wcześniejsze laby nadal
korzystają z preview.

## 1. `var`: kompilator ustala typ

```java
var accountNumber = "PLN-1001";
var balance = new BigDecimal("1000.00");
```

Kompilator ustala:

```text
accountNumber ma typ String
balance ma typ BigDecimal
```

To nadal statyczne typowanie. Typ jest ustalony podczas kompilacji i nie
zmienia się podczas działania programu.

To się skompiluje:

```java
var accountNumber = "PLN-1001";
accountNumber = "PLN-1002";
```

To się nie skompiluje:

```java
var accountNumber = "PLN-1001";
accountNumber = 123;
```

`var` nie oznacza również `final`: wartość można zmieniać, jeśli pasuje do
ustalonego typu. Niezmienność przypisania wymaga `final var`.

### Gdzie można używać `var`?

W tym labie używamy go dla zmiennych lokalnych z inicjalizatorem.
Nie można używać go jako typu pola, typu wyniku metody ani zwykłego
parametru metody.

Te przykłady są niepoprawne:

```java
var balance = null;                  // Nie można ustalić typu z samego null.
var accountNumber;                  // Brak inicjalizatora.
public var report() { ... }         // Niepoprawny typ wyniku metody.
public void report(var account) {}  // Niepoprawny zwykły parametr metody.
```

Osobną regułą jest obsługa `var` w parametrach lambd od Java 11.
Nie potrzebujemy jej w tym ćwiczeniu.

Używaj `var`, gdy typ jest łatwy do odczytania z prawej strony. Nie trzeba
zamieniać każdej deklaracji na `var`.

## 2. Text block: jeden napis zapisany w wielu wierszach

```java
var report = """
        Account report
        Account: PLN-1001
        Balance: 1000.00 PLN
        """;
```

`report` ma typ `String`. Text block nie jest nowym typem danych.

Ważne zasady:

- po otwierającym `"""` musi wystąpić koniec wiersza,
- kompilator usuwa wspólne, nieistotne wcięcie,
- końce wierszy w treści są normalizowane do `\n`, także na Windows,
- w tym przykładzie napis kończy się `\n`, ponieważ zamykające `"""`
  jest w osobnym wierszu.

## 3. Text block nie interpoluje zmiennych

Java nie podstawi automatycznie wartości do `${accountNumber}`.
W tym labie użyjemy `%s` i metody `String.formatted(...)`:

```java
var report = """
        Account: %s
        """.formatted(accountNumber);
```

`formatted(...)` jest dostępne od Java 15.

## 4. Klasa do wklejenia

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\text\AccountReportFormatter.java
```

Wklej:

```java
package com.delta.bank.lab.java21.text;

import java.math.BigDecimal;
import java.util.Objects;

public class AccountReportFormatter {

    public String format(String accountNumber, BigDecimal balance) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(balance, "balance must not be null");

        var balanceText = balance.toPlainString();
        var report = """
                Account report
                Account: %s
                Balance: %s PLN
                """.formatted(accountNumber, balanceText);

        return report;
    }
}
```

Przeczytaj metodę w tej kolejności:

1. Sprawdza, czy argumenty nie są `null`.
2. `balanceText` ma ustalony przez kompilator typ `String`.
3. Text block opisuje układ raportu.
4. `formatted(...)` podstawia numer konta i saldo do dwóch `%s`.
5. Metoda zwraca zwykły `String`.

`toPlainString()` zachowuje skalę przekazanego `BigDecimal` i nie używa
notacji wykładniczej. Nie wymuszamy w tym ćwiczeniu dwóch miejsc po
przecinku: `new BigDecimal("1000.00")` daje `"1000.00"`, a
`new BigDecimal("1000")` daje `"1000"`.

## 5. Testy Spocka do wklejenia

Utwórz plik:

```text
src\test\groovy\com\delta\bank\lab\java21\text\AccountReportFormatterSpec.groovy
```

Wklej:

```groovy
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
```

W kodzie Groovy oczekiwany napis używa `${expected}`. To składnia Groovy,
nie Javy. Java w naszej klasie używa `%s` i `formatted(...)`.

Pierwszy test celowo zapisuje oczekiwany wynik z jawnymi `\n`, a nie jako
wielowierszowy napis. Dzięki temu dokładnie widać końce wierszy, w tym
ostatni znak nowej linii.

## 6. Uruchomienie

Z katalogu projektu w Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.text.AccountReportFormatterSpec"
```

W PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.text.AccountReportFormatterSpec"
```

Ten spec nie uruchamia Springa i nie wymaga bazy danych.

## 7. Dwa krótkie eksperymenty

Wykonuj je pojedynczo i po każdym przywróć początkowy kod.

### Eksperyment A: `var` nie jest typem dynamicznym

Bezpośrednio po deklaracji `balanceText` dopisz:

```java
balanceText = 123;
```

Kompilacja powinna zgłosić błąd: nie można przypisać liczby do zmiennej
typu `String`. Usuń tę linię.

### Eksperyment B: końcowy znak nowej linii

Zmień text block tak, aby zamykające `"""` znalazło się zaraz po `PLN`:

```java
var report = """
        Account report
        Account: %s
        Balance: %s PLN""".formatted(accountNumber, balanceText);
```

Testy porównujące raport przestaną przechodzić: wynik nie będzie już
zawierał końcowego `\n`. Przywróć zamykające `"""` do osobnego wiersza.

## 8. Pytania rekrutacyjne

### Czy `var` oznacza dynamiczne typowanie?

Nie. Kompilator ustala statyczny typ zmiennej na podstawie inicjalizatora.

### Czy `var` oznacza `final`?

Nie. Do zablokowania ponownego przypisania można użyć `final var`.

### Czy text block ma inny typ niż zwykły napis?

Nie. Wynikiem jest `String`.

### Czy text block automatycznie podstawia zmienne?

Nie. W tym labie robimy to jawnie przez `formatted(...)`.

### Czy text block zawsze kończy się znakiem nowej linii?

Nie. Zależy to od zapisu treści i położenia zamykającego delimitera.

### Czy te funkcje wymagają preview w JDK 21?

Nie. `var` i text blocks są już finalnymi elementami języka.

## Do zapamiętania

```text
var = kompilator ustala typ zmiennej lokalnej.
Text block = czytelny zapis wielowierszowego Stringa.
formatted(...) = jawne podstawienie wartości do formatu.
```
