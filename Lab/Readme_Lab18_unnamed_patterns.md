# Lab 18 — Unnamed Patterns w Java 21

## Cel laba

W tym labie ćwiczymy unnamed patterns, czyli wzorce `_`, wprowadzone jako
preview w Java 21. Użyjemy ich w record patterns, aby jawnie zignorować
komponenty rekordu, których dana gałąź nie potrzebuje.

Przykład:

```java
case DepositEvent(_, var amount) -> ...
```

Wzorzec `_` oznacza: „ten komponent musi być obecny, ale nie będę go
odczytywać ani wiązać z nazwą zmiennej”.

To ćwiczenie rozwija Lab1 i Lab17: wykorzystuje rekordy, sealed hierarchy
i pattern matching w `switch`. Nie zmieniamy produkcyjnego mini-banku.

## Ważne: preview w Java 21

Unnamed patterns and variables były preview w JDK 21. W Java 21 wymagają
`--enable-preview` podczas kompilacji i uruchamiania.

W obecnym `build.gradle` projektu te flagi są już skonfigurowane:

```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['--enable-preview', '--release', '21']
}

tasks.withType(Test).configureEach {
    jvmArgs '--enable-preview'
}
```

Nie dodawaj ich ponownie. Uruchamiamy test przez Gradle, który ma już
flagę dla testowego JVM. Jeśli uruchamiasz klasę `main()` bezpośrednio
w IntelliJ, konfiguracja uruchomieniowa również wymaga VM option
`--enable-preview`.

Unnamed patterns stały się funkcją finalną w Java 22. W tym labie
trzymamy się zachowania preview z JDK 21.

## 1. Problem: wymagana zmienna, której nie używamy

Przed unnamed patterns można było zapisać wzorzec tak:

```java
case DepositEvent(var accountNumber, var amount) ->
        "Deposit: " + amount;
```

Zmienna `accountNumber` nie jest używana. Może to rozpraszać, a IDE może
zgłaszać ostrzeżenie.

W Java 21 preview można jawnie zaznaczyć, że komponent pomijamy:

```java
case DepositEvent(_, var amount) ->
        "Deposit: " + amount;
```

Podkreślenie nie tworzy zmiennej. Nie można później odczytać wartości
pominiętego komponentu.

## 2. `_` nie jest zwykłą nazwą zmiennej

Nie używaj `_` jak identyfikatora:

```java
var _ = "value"; // Niepoprawne: _ nie jest nazwą zmiennej.
```

W tym labie używamy `_` wyłącznie we wzorcach record patterns. Java 21
preview udostępnia też unnamed variables w wybranych kontekstach, ale
skupiamy się na wzorcach, żeby ograniczyć liczbę nowych zasad.

## 3. Sealed hierarchia zdarzeń

Zdarzenia:

```text
StatementEvent
├── DepositEvent(accountNumber, amount)
├── WithdrawalEvent(accountNumber, amount)
└── TransferEvent(sourceAccount, targetAccount, amount)
```

Processor opisuje typ i kwotę. Nie potrzebuje numerów rachunków, więc
rozpakowuje je jako `_`.

## 4. Pliki do wklejenia

Utwórz pakiet:

```text
src\main\java\com\delta\bank\lab\java21\unnamedpatterns
```

### `StatementEvent.java`

```java
package com.delta.bank.lab.java21.unnamedpatterns;

public sealed interface StatementEvent
        permits DepositEvent, WithdrawalEvent, TransferEvent {
}
```

### `DepositEvent.java`

```java
package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record DepositEvent(String accountNumber, BigDecimal amount)
        implements StatementEvent {

    public DepositEvent {
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

### `WithdrawalEvent.java`

```java
package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record WithdrawalEvent(String accountNumber, BigDecimal amount)
        implements StatementEvent {

    public WithdrawalEvent {
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

### `TransferEvent.java`

```java
package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;
import java.util.Objects;

public record TransferEvent(
        String sourceAccount,
        String targetAccount,
        BigDecimal amount
) implements StatementEvent {

    public TransferEvent {
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

## 5. Describer z unnamed patterns

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\unnamedpatterns\StatementEventDescriber.java
```

Wklej:

```java
package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;

public class StatementEventDescriber {

    private static final BigDecimal REVIEW_THRESHOLD =
            new BigDecimal("1000.00");

    public String describe(StatementEvent event) {
        return switch (event) {
            case null -> "No statement event";
            case DepositEvent(_, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    "Large deposit: " + amount.toPlainString() + " PLN";
            case DepositEvent(_, var amount) ->
                    "Deposit: " + amount.toPlainString() + " PLN";
            case WithdrawalEvent(_, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    "Large withdrawal: " + amount.toPlainString() + " PLN";
            case WithdrawalEvent(_, var amount) ->
                    "Withdrawal: " + amount.toPlainString() + " PLN";
            case TransferEvent(_, _, var amount) ->
                    "Transfer: " + amount.toPlainString() + " PLN";
        };
    }
}
```

Czytaj wzorce:

```text
DepositEvent(_, var amount)
             └─ accountNumber pomijamy
                 └─ amount wiążemy ze zmienną

TransferEvent(_, _, var amount)
               └─ źródło i cel pomijamy
                           └─ kwotę odczytujemy
```

Kompilator nadal sprawdza kształt i typy komponentów. `_` nie oznacza,
że komponent może być nieobecny albo `null`; mówi tylko, że nie wiążemy
go z nazwą.

Szczególne przypadki z `when` są przed ogólnymi wzorcami danego typu.
Sealed hierarchy pozwala pominąć `default`, a `case null` jawnie określa
zachowanie dla `null`.

## 6. Spec Spocka

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\unnamedpatterns\StatementEventDescriberSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.unnamedpatterns

import spock.lang.Specification
import spock.lang.Unroll

class StatementEventDescriberSpec extends Specification {

    private final StatementEventDescriber describer =
            new StatementEventDescriber()

    @Unroll
    def "describes event as #expected"() {
        expect:
        describer.describe(event) == expected

        where:
        event                                                                | expected
        new DepositEvent("PLN-1001", new BigDecimal("250.00"))              | "Deposit: 250.00 PLN"
        new DepositEvent("PLN-1001", new BigDecimal("1000.00"))             | "Large deposit: 1000.00 PLN"
        new WithdrawalEvent("PLN-1002", new BigDecimal("80.00"))            | "Withdrawal: 80.00 PLN"
        new WithdrawalEvent("PLN-1002", new BigDecimal("1500.00"))          | "Large withdrawal: 1500.00 PLN"
        new TransferEvent("PLN-1001", "PLN-1002", new BigDecimal("75.00"))  | "Transfer: 75.00 PLN"
        null                                                                 | "No statement event"
    }

    def "validates record components even when patterns ignore them"() {
        when:
        new DepositEvent(" ", new BigDecimal("10.00"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "accountNumber must not be blank"
    }
}
```

Test weryfikuje wynik, nie sposób kompilacji wzorca. Przykłady `_` w kodzie
produkcyjnym laba pokazują, że komponenty rachunku nie są wiązane z
nieużywanymi zmiennymi.

## 7. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.unnamedpatterns.StatementEventDescriberSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.unnamedpatterns.StatementEventDescriberSpec"
```

Jeśli Gradle zgłosi, że preview feature jest niedozwolone, sprawdź, czy
używasz JDK 21 i czy konfiguracja kompilacji oraz testowego JVM zawiera
`--enable-preview`. W projekcie ustawienia te powinny już pochodzić z
Lab 9.

## 8. Eksperyment: zastąp `_` nazwą

Zmień:

```java
case DepositEvent(_, var amount) ->
```

na:

```java
case DepositEvent(var accountNumber, var amount) ->
```

Kod nadal się kompiluje, ale `accountNumber` pozostaje nieużywany.
Przywróć `_`, aby wzorzec komunikował intencję: komponent jest celowo
ignorowany.

## 9. Eksperyment: jawny typ zamiast `var`

Wzorzec:

```java
case DepositEvent(_, var amount) ->
```

można zapisać z jawnym typem komponentu:

```java
case DepositEvent(_, BigDecimal amount) ->
```

`var` pozwala wywnioskować typ komponentu z deklaracji rekordu.
W tym ćwiczeniu oba warianty działają; wybierz zapis czytelniejszy
w kontekście.

## Pytania kontrolne

1. Co oznacza `_` w record pattern?
2. Czy można później odczytać komponent dopasowany przez `_`?
3. Czy `_` zmienia walidację konstruktora rekordu?
4. Czym unnamed pattern różni się od utworzenia zmiennej o nazwie `_`?
5. Dlaczego ogólne przypadki z `when` powinny być po szczególnych?
6. Czy unnamed patterns były finalne w Java 21?
7. Kiedy ta funkcja stała się finalna?

Do zapamiętania:

```text
_ = dopasuj, ale nie wiąż z nazwą
record pattern = rozpakuj komponenty rekordu
when = dodatkowy warunek wzorca
JDK 21 unnamed patterns = preview, potrzebuje --enable-preview
```
