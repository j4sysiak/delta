# Lab 17 — Pattern matching w `switch` (Java 21)

## Cel laba

W Lab1 poznałeś sealed types, records i pattern matching. W Lab12 używałeś
switch expressions dla enumów. Teraz połączymy te elementy:

- `switch` dopasowujący typy,
- record patterns rozpakowujące komponenty rekordu,
- `when` guards, czyli dodatkowe warunki wzorca,
- exhaustiveness dla sealed hierarchii,
- jawne obsłużenie `null`.

Pattern matching for `switch` jest finalny od Java 21 i nie wymaga preview.
Ćwiczenie jest samodzielne: nie zmieniamy kodu produkcyjnego mini-banku,
nie używamy Springa ani bazy danych.

## 1. Sealed hierarchia zdarzeń bankowych

Zdarzenia bankowe mają wspólny typ, ale znany zestaw wariantów:

```text
BankEvent
├── DepositRecorded
├── WithdrawalRecorded
└── TransferRecorded
```

Interfejs `sealed` ogranicza implementacje do wymienionych typów.
Kompilator może dzięki temu sprawdzić, czy switch obsługuje wszystkie
warianty.

## 2. Dopasowanie typu

Switch może dopasować konkretny typ i nadać dopasowanemu obiektowi nazwę:

```java
return switch (event) {
    case DepositRecorded deposit -> "Deposit";
    case WithdrawalRecorded withdrawal -> "Withdrawal";
    case TransferRecorded transfer -> "Transfer";
};
```

W każdej gałęzi zmienna ma konkretny typ. Nie potrzeba osobnego
`instanceof`, ręcznego castowania ani `break`.

## 3. Record patterns

Rekord można rozpakować bezpośrednio we wzorcu:

```java
case DepositRecorded(var accountNumber, var amount) ->
        "Deposit to " + accountNumber + ": " + amount;
```

`accountNumber` i `amount` są wiązane z komponentami rekordu. Ich typy
kompilator wywnioskuje z deklaracji `DepositRecorded`.

## 4. `when` guards

Do wzorca można dodać warunek:

```java
case DepositRecorded(var accountNumber, var amount)
        when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
        "Large deposit";
```

Guard nie zastępuje wzorca. Najpierw musi pasować typ i kształt rekordu,
a dopiero potem oceniany jest warunek `when`.

Kolejność gałęzi ma znaczenie. Szczególny przypadek z guardem umieść
przed ogólnym przypadkiem tego samego typu. Nieosłonięty wzorzec
`case DepositRecorded(...)` pasuje do każdego depozytu i sprawiłby, że
późniejszy przypadek tego typu byłby nieosiągalny.

## 5. Exhaustiveness i `null`

Jeżeli switch obsługuje każdy wariant sealed hierarchii, `default` nie
jest potrzebny. Gdy dodasz nowy dozwolony typ zdarzenia, kompilator
wskaże switch wymagający aktualizacji.

`null` nie jest jedną z dozwolonych implementacji. Switch na referencji
`null` zwykle rzuca `NullPointerException`, chyba że jawnie podasz
`case null`. W tym labie jawnie zwrócimy opis braku zdarzenia.

## 6. Typy zdarzeń do wklejenia

Utwórz pakiet:

```text
src\main\java\com\delta\bank\lab\java21\switchpatterns
```

### `BankEvent.java`

```java
package com.delta.bank.lab.java21.switchpatterns;

public sealed interface BankEvent
        permits DepositRecorded, WithdrawalRecorded, TransferRecorded {
}
```

### `DepositRecorded.java`

```java
package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record DepositRecorded(String accountNumber, BigDecimal amount)
        implements BankEvent {

    public DepositRecorded {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

### `WithdrawalRecorded.java`

```java
package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record WithdrawalRecorded(String accountNumber, BigDecimal amount)
        implements BankEvent {

    public WithdrawalRecorded {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

### `TransferRecorded.java`

```java
package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferRecorded(
        String sourceAccount,
        String targetAccount,
        BigDecimal amount
) implements BankEvent {

    public TransferRecorded {
        Objects.requireNonNull(sourceAccount, "sourceAccount must not be null");
        Objects.requireNonNull(targetAccount, "targetAccount must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (sourceAccount.isBlank()) {
            throw new IllegalArgumentException("sourceAccount must not be blank");
        }
        if (targetAccount.isBlank()) {
            throw new IllegalArgumentException("targetAccount must not be blank");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

Wszystkie kwoty są dodatnie; rodzaj zdarzenia mówi, czy jest to wpłata,
wypłata czy przelew. Nie modelujemy tutaj księgowego znaku kwoty.

## 7. Processor do wklejenia

Utwórz `BankEventDescriber.java`:

```text
src\main\java\com\delta\bank\lab\java21\switchpatterns\BankEventDescriber.java
```

```java
package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;

public class BankEventDescriber {

    private static final BigDecimal REVIEW_THRESHOLD =
            new BigDecimal("1000.00");

    public String describe(BankEvent event) {
        return switch (event) {
            case null -> "No bank event";
            case DepositRecorded(var accountNumber, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    "Large deposit to " + accountNumber
                            + ": " + amount.toPlainString() + " PLN";
            case DepositRecorded(var accountNumber, var amount) ->
                    "Deposit to " + accountNumber
                            + ": " + amount.toPlainString() + " PLN";
            case WithdrawalRecorded(var accountNumber, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    "Large withdrawal from " + accountNumber
                            + ": " + amount.toPlainString() + " PLN";
            case WithdrawalRecorded(var accountNumber, var amount) ->
                    "Withdrawal from " + accountNumber
                            + ": " + amount.toPlainString() + " PLN";
            case TransferRecorded(var source, var target, var amount) ->
                    "Transfer from " + source + " to " + target
                            + ": " + amount.toPlainString() + " PLN";
        };
    }
}
```

Tutaj:

- `case DepositRecorded(...)` rozpoznaje typ i rozpakowuje rekord,
- `when` wybiera duże wpłaty i wypłaty do opisu wymagającego przeglądu,
- ogólne przypadki wpłaty i wypłaty muszą być po guardach,
- `case null` definiuje zachowanie dla braku zdarzenia,
- nie ma `default`, bo sealed hierarchia jest w całości obsłużona.

## 8. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\switchpatterns\BankEventDescriberSpec.groovy
```

```groovy
package com.delta.bank.lab.java21.switchpatterns

import spock.lang.Specification
import spock.lang.Unroll

class BankEventDescriberSpec extends Specification {

    private final BankEventDescriber describer = new BankEventDescriber()

    @Unroll
    def "describes #event"() {
        expect:
        describer.describe(event) == expected

        where:
        event                                                               | expected
        new DepositRecorded("PLN-1001", new BigDecimal("250.00"))           | "Deposit to PLN-1001: 250.00 PLN"
        new DepositRecorded("PLN-1001", new BigDecimal("1000.00"))          | "Large deposit to PLN-1001: 1000.00 PLN"
        new WithdrawalRecorded("PLN-1002", new BigDecimal("80.00"))        | "Withdrawal from PLN-1002: 80.00 PLN"
        new WithdrawalRecorded("PLN-1002", new BigDecimal("1500.00"))       | "Large withdrawal from PLN-1002: 1500.00 PLN"
        new TransferRecorded("PLN-1001", "PLN-1002", new BigDecimal("75.00")) | "Transfer from PLN-1001 to PLN-1002: 75.00 PLN"
        null                                                                | "No bank event"
    }

    def "rejects blank account number"() {
        when:
        new DepositRecorded(" \t ", new BigDecimal("10.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }

    def "rejects non-positive transfer amount"() {
        when:
        new TransferRecorded(
                "PLN-1001",
                "PLN-1002",
                new BigDecimal("0.00")
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be positive"
    }
}
```

## 9. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.switchpatterns.BankEventDescriberSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.switchpatterns.BankEventDescriberSpec"
```

Kod switch pattern matching nie wymaga preview. Test nie potrzebuje Springa,
bazy ani sieci.

## 10. Eksperyment: exhaustiveness kompilatora

Dodaj do `BankEvent.java` nowy dozwolony rekord, np. `FeeCharged`, a potem
ponownie skompiluj projekt.

Kompilator zgłosi, że `BankEventDescriber` nie obsługuje nowego wariantu.
Dodaj osobną gałąź `case FeeCharged(...) -> ...`, a błąd zniknie.

Po eksperymencie usuń dodatkowy typ, jeśli nie chcesz go zostawiać.
Nie dodawaj ogólnego `default`, bo wtedy kompilator nie wymusi jawnego
opisania nowego wariantu.

## 11. Eksperyment: kolejność guardów

Przenieś ogólną gałąź:

```java
case DepositRecorded(var accountNumber, var amount) -> ...
```

nad gałąź z `when`. Kompilator powinien odrzucić switch, ponieważ ogólny
wzorzec dopasuje każdy `DepositRecorded`, przez co wariant z guardem stanie
się nieosiągalny.

Przywróć szczególną gałąź z `when` przed ogólną.

## Pytania kontrolne

1. Co daje `sealed` switchowi dopasowującemu warianty?
2. Czym pattern matching for switch różni się od ręcznego `instanceof`
   i castowania?
3. Co robi record pattern w `case DepositRecorded(...)`?
4. Kiedy guard `when` jest sprawdzany?
5. Dlaczego szczególny wzorzec z guardem powinien wystąpić przed ogólnym?
6. Czy exhaustive switch na typie referencyjnym automatycznie obsługuje
   `null`?
7. Dlaczego jawny `case null` może być czytelniejszy niż domyślny NPE?
8. Czy pattern matching for switch w Java 21 wymaga preview?

Do zapamiętania:

```text
sealed hierarchy → znany zestaw wariantów
type pattern     → rozpoznaje typ i wiąże zmienną
record pattern   → rozpakowuje komponenty rekordu
when             → dodatkowy warunek wzorca
exhaustive switch → kompilator pilnuje wszystkich wariantów
```
