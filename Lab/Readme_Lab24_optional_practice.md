# Lab 24 — `Optional` w praktyce

## Cel

`Optional` nie jest nowością Java 21 — pojawił się w Java 8, a kilka
przydatnych metod dodano w Java 9. W tym labie uporządkujemy użycie
`Optional` na przykładzie wyszukiwania konta.

Przećwiczysz:

- jawne reprezentowanie braku wyniku,
- `map` i `flatMap`,
- `orElse` kontra `orElseGet`,
- `or(...)`, `ifPresentOrElse(...)` i `stream()` z Java 9,
- sytuacje, w których lepiej nie używać `Optional`.

Bez Springa, bazy danych, wątków i preview. Nie zmieniamy kodu
produkcyjnego mini-banku.

## 1. `Optional<T>` oznacza: wartość może być nieobecna

Metoda wyszukująca może zwrócić:

```java
Optional<AccountSummary>
```

Wynik jest jednym z dwóch przypadków:

```text
Optional.of(account) → znaleziono konto
Optional.empty()     → nie znaleziono konta
```

To lepszy kontrakt niż zwracanie `null`, jeśli brak wyniku jest normalną
możliwością.

## 2. Tworzenie Optional

```java
Optional.of(value)
```

Używaj, gdy `value` na pewno nie jest `null`. Jeśli jest `null`,
`of(...)` rzuci `NullPointerException`.

```java
Optional.ofNullable(value)
```

Używaj, gdy źródło może zwrócić `null`:

```java
Optional<AccountSummary> result = Optional.ofNullable(nullableAccount);
```

```java
Optional.empty()
```

Jawnie oznacza brak wartości.

## 3. Odczyt wartości bez `get()`

Nie wywołuj bezwarunkowo:

```java
optional.get()
```

Jeśli wartość jest pusta, metoda rzuci `NoSuchElementException`.
Zwykle czytelniejsze są:

```java
optional.orElse(defaultValue)
optional.orElseGet(() -> calculateDefault())
optional.orElseThrow(() -> new IllegalStateException("Account required"))
```

Albo przekształć zawartość:

```java
optional.map(AccountSummary::owner)
```

W tym labie użyjemy `get()` tylko w testach po sprawdzeniu, że wartość
istnieje; w kodzie produkcyjnym będą bezpieczniejsze formy.

## 4. `map`: przekształć zawartość, jeśli istnieje

```java
Optional<String> owner = accountOptional.map(AccountSummary::owner);
```

- gdy Optional zawiera konto, funkcja przekształca je do nazwy właściciela,
- gdy jest pusty, wynik pozostaje pusty.

`map` nie opakowuje wyniku, który już jest `Optional`. Jeśli mapper sam
zwraca `Optional`, użyj `flatMap`.

## 5. `flatMap`: unikaj `Optional<Optional<T>>`

Załóżmy, że konto może mieć alias, który sam może być nieobecny:

```java
Optional<Optional<String>> nested =
        accountOptional.map(AccountSummary::alias);
```

To zwykle nie jest pożądany kształt. Zamiast tego:

```java
Optional<String> alias =
        accountOptional.flatMap(AccountSummary::alias);
```

`flatMap` łączy oba poziomy opcjonalności w jeden `Optional`.
Funkcja przekazana do `flatMap` nie może zwrócić `null`; w przeciwnym
razie zostanie zgłoszony `NullPointerException`.

## 6. `orElse` kontra `orElseGet`

```java
optional.orElse(createFallback())
```

Argument `createFallback()` jest obliczany od razu, nawet jeśli Optional
zawiera wartość.

```java
optional.orElseGet(() -> createFallback())
```

Supplier jest wywoływany dopiero wtedy, gdy Optional jest pusty.

Używaj `orElse` dla taniej, gotowej wartości. Dla kosztownego albo
mającego efekty uboczne obliczenia domyślnego użyj `orElseGet`.
W ogólności nie umieszczaj ważnych efektów ubocznych w żadnej z tych
form — lepiej wyrazić je jawnie.

## 7. Metody Optional dodane w Java 9

### `or(...)`

Pozwala podać alternatywne źródło, które samo zwraca Optional:

```java
Optional<AccountSummary> account =
        primaryLookup.find(accountNumber)
                .or(() -> archiveLookup.find(accountNumber));
```

Drugie wyszukiwanie jest wykonywane tylko wtedy, gdy pierwsze Optional
jest puste.

### `ifPresentOrElse(...)`

Obsługuje oba przypadki:

```java
optional.ifPresentOrElse(
        account -> process(account),
        () -> recordMissingAccount()
);
```

Używaj tego, gdy chcesz wykonać akcję w obu przypadkach. Jeśli potrzebujesz
zwrócić nową wartość, `map`, `orElse` lub `orElseThrow` zwykle będą
czytelniejsze.

### `stream()`

Zamienia:

```text
Optional<T> → Stream<T> zawierający zero albo jeden element
```

Przykład spłaszczenia kolekcji Optional:

```java
List<String> aliases = accounts.stream()
        .map(AccountSummary::alias)
        .flatMap(Optional::stream)
        .toList();
```

Puste aliasy nie dodają elementu do wyniku.

## 8. Rekord i serwis do wklejenia

Utwórz pakiet:

```text
src\main\java\com\delta\bank\lab\java21\optional
```

### `AccountSummary.java`

```java
package com.delta.bank.lab.java21.optional;

import java.util.Objects;
import java.util.Optional;

public record AccountSummary(
        String accountNumber,
        String owner,
        Optional<String> alias
) {

    public AccountSummary {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(alias, "alias must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        alias.ifPresent(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException("alias must not be blank");
            }
        });
    }
}
```

Tutaj `Optional` jako pole rekordu jest świadomym wyborem dla ćwiczenia.
W typowym modelu domenowym/API `Optional` najczęściej stosuje się jako
typ wyniku metody, a nie jako pole DTO, encji JPA, parametr metody lub
element kolekcji.

### `AccountDirectory.java`

```java
package com.delta.bank.lab.java21.optional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AccountDirectory {

    private final Map<String, AccountSummary> accountsByNumber;

    public AccountDirectory(List<AccountSummary> accounts) {
        Objects.requireNonNull(accounts, "accounts must not be null");

        for (var account : accounts) {
            Objects.requireNonNull(account, "account must not be null");
        }

        this.accountsByNumber = accounts.stream()
                .collect(Collectors.toUnmodifiableMap(
                        AccountSummary::accountNumber,
                        account -> account
                ));
    }

    public Optional<AccountSummary> find(String accountNumber) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        return Optional.ofNullable(accountsByNumber.get(accountNumber));
    }

    public Optional<String> findOwner(String accountNumber) {
        return find(accountNumber).map(AccountSummary::owner);
    }

    public Optional<String> findAlias(String accountNumber) {
        return find(accountNumber).flatMap(AccountSummary::alias);
    }

    public AccountSummary findOrElse(
            String accountNumber,
            AccountSummary fallback
    ) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        return find(accountNumber).orElse(fallback);
    }

    public AccountSummary findOrElseGet(
            String accountNumber,
            Supplier<AccountSummary> fallback
    ) {
        Objects.requireNonNull(fallback, "fallback must not be null");
        return find(accountNumber).orElseGet(fallback);
    }

    public AccountSummary findInPrimaryOrArchive(
            String accountNumber,
            AccountDirectory archive
    ) {
        Objects.requireNonNull(archive, "archive must not be null");

        return find(accountNumber)
                .or(() -> archive.find(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found: " + accountNumber
                ));
    }

    public List<String> aliasesFor(List<String> accountNumbers) {
        Objects.requireNonNull(accountNumbers, "accountNumbers must not be null");

        return accountNumbers.stream()
                .map(this::findAlias)
                .flatMap(Optional::stream)
                .toList();
    }
}
```

Konstruktor kopiuje dane do niemodyfikowalnej mapy. Duplikaty numerów kont
są błędem i `toUnmodifiableMap` zgłosi wyjątek zamiast po cichu zgubić
jeden wpis.

## 9. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\optional\AccountDirectorySpec.groovy
```

```groovy
package com.delta.bank.lab.java21.optional

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class AccountDirectorySpec extends Specification {

    def "returns a present Optional when account exists"() {
        when:
        def result = directory().find("PLN-1001")

        then:
        result.isPresent()
        result.get().owner() == "Anna Kowalska"
    }

    def "returns an empty Optional when account does not exist"() {
        expect:
        directory().find("PLN-9999").isEmpty()
    }

    def "map transforms a present value and leaves an empty result empty"() {
        expect:
        directory().findOwner("PLN-1001") == Optional.of("Anna Kowalska")
        directory().findOwner("PLN-9999").isEmpty()
    }

    def "flatMap avoids a nested Optional when alias exists"() {
        expect:
        directory().findAlias("PLN-1001") == Optional.of("anna-main")
    }

    def "flatMap returns empty when account has no alias"() {
        expect:
        directory().findAlias("PLN-1002").isEmpty()
    }

    def "orElse returns fallback for a missing account"() {
        given:
        def fallback = new AccountSummary(
                "UNKNOWN",
                "Unknown owner",
                Optional.empty()
        )

        expect:
        directory().findOrElse("PLN-9999", fallback).is(fallback)
    }

    def "orElseGet evaluates fallback only when value is absent"() {
        given:
        def fallbackCalls = new AtomicInteger()
        def fallback = {
            fallbackCalls.incrementAndGet()
            new AccountSummary("FALLBACK", "Fallback owner", Optional.empty())
        }

        when:
        def found = directory().findOrElseGet("PLN-1001", fallback)

        then:
        found.accountNumber() == "PLN-1001"
        fallbackCalls.get() == 0

        when:
        def missing = directory().findOrElseGet("PLN-9999", fallback)

        then:
        missing.accountNumber() == "FALLBACK"
        fallbackCalls.get() == 1
    }

    def "orElse evaluates its argument even when Optional contains a value"() {
        given:
        def fallbackCalls = new AtomicInteger()
        def fallback = {
            fallbackCalls.incrementAndGet()
            new AccountSummary("FALLBACK", "Fallback owner", Optional.empty())
        }

        when:
        def found = directory().find("PLN-1001")
                .orElse(fallback.call())

        then:
        found.accountNumber() == "PLN-1001"
        fallbackCalls.get() == 1
    }

    def "or tries archive only when primary lookup is empty"() {
        given:
        def primary = directory()
        def archive = new AccountDirectory([
                new AccountSummary(
                        "PLN-2001",
                        "Archived owner",
                        Optional.empty()
                )
        ])

        expect:
        primary.findInPrimaryOrArchive("PLN-1001", archive).owner() ==
                "Anna Kowalska"
        primary.findInPrimaryOrArchive("PLN-2001", archive).owner() ==
                "Archived owner"
    }

    def "orElseThrow reports a missing account"() {
        when:
        directory().findInPrimaryOrArchive(
                "PLN-9999",
                new AccountDirectory([])
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Account not found: PLN-9999"
    }

    def "Optional stream flattens present aliases and skips empty ones"() {
        expect:
        directory().aliasesFor(["PLN-1001", "PLN-1002", "PLN-9999"]) ==
                ["anna-main"]
    }

    def "rejects duplicate account numbers"() {
        given:
        def duplicateAccounts = [
                new AccountSummary("PLN-1001", "Anna", Optional.empty()),
                new AccountSummary("PLN-1001", "Another owner", Optional.empty())
        ]

        when:
        new AccountDirectory(duplicateAccounts)

        then:
        thrown(IllegalStateException)
    }

    private static AccountDirectory directory() {
        return new AccountDirectory([
                new AccountSummary(
                        "PLN-1001",
                        "Anna Kowalska",
                        Optional.of("anna-main")
                ),
                new AccountSummary(
                        "PLN-1002",
                        "Jan Nowak",
                        Optional.empty()
                )
        ])
    }
}
```

Test `orElse` celowo pokazuje pułapkę: argument jest obliczany przed
wywołaniem metody. `orElseGet` przyjmuje supplier, więc może odroczyć
obliczenie do chwili, kiedy Optional jest pusty.

`fallback.call()` jest wywołaniem Groovy closure w teście. Kod Javy
otrzymuje gotowy obiekt `AccountSummary`.

## 10. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.optional.AccountDirectorySpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.optional.AccountDirectorySpec"
```

Test nie wymaga Springa, bazy ani sieci.

## 11. Czego unikać?

### `Optional` jako parametr metody

Zwykle czytelniej jest przekazać wartość albo `null` zgodnie z jasno
udokumentowanym kontraktem, niż wymagać, by każdy wywołujący opakowywał
argument w `Optional`.

### `Optional` w polach encji i DTO

`Optional` zaprojektowano głównie jako typ wyniku metody. W encjach JPA,
polach DTO i formatach transportowych mogą pojawić się problemy z
frameworkami, serializacją i mapowaniem.

W tym labie pole `alias` jest użyte celowo, aby przećwiczyć `flatMap`.
W zwykłej encji lub DTO wybrałbyś reprezentację zgodną z kontraktem
frameworka, często `String` dopuszczający `null` albo osobny typ.

### `Optional` w kolekcji

Zamiast:

```java
List<Optional<String>>
```

często lepiej spłaszczyć obecne wartości do:

```java
List<String>
```

W Java 9+ pomaga `Optional.stream()`.

### `get()` bez sprawdzenia

Jeśli potrzebujesz `get()`, najpierw upewnij się, że Optional jest obecny.
W kodzie produkcyjnym zwykle wygodniej użyć `map`, `orElse`,
`orElseGet` lub `orElseThrow`.

## Pytania kontrolne

1. Kiedy użyć `Optional.of`, a kiedy `ofNullable`?
2. Co zwraca `map`, jeśli Optional jest pusty?
3. Kiedy wybrać `flatMap` zamiast `map`?
4. Jaka jest różnica w ewaluacji pomiędzy `orElse` i `orElseGet`?
5. Kiedy Supplier przekazany do `or(...)` jest wywoływany?
6. Co zwraca `Optional.stream()` dla obecnej i pustej wartości?
7. Dlaczego `Optional` zwykle nie jest najlepszym typem pola encji JPA?

Do zapamiętania:

```text
Optional<T> → wynik może być nieobecny
map         → przekształca zawartość
flatMap     → łączy zagnieżdżone Optional
orElse      → argument obliczany od razu
orElseGet   → Supplier wywołany tylko dla empty
or / stream / ifPresentOrElse → przydatne metody dodane w Java 9
```
