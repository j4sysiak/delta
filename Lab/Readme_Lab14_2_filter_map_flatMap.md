# Lab14.2 — `filter`, `map` i `flatMap`

## Cel

W Lab14.1 poznałeś kształt pipeline'u i kiedy jest wykonywany.
Teraz skupimy się na trzech operacjach pośrednich:

- `filter` — wybiera elementy, nie zmieniając ich typu,
- `map` — przekształca każdy element w jeden wynik,
- `flatMap` — przekształca każdy element w strumień, a potem łączy te
  strumienie w jeden.

Pracujemy na `Transaction` i `TransactionType` utworzonych w Lab14.1,
w pakiecie:

```text
com.delta.bank.lab.java21.streamdeepdive
```

Nie twórz ponownie tych dwóch klas. Kod produkcyjny mini-banku pozostaje
bez zmian.

## 1. `filter`: wybierz elementy

```java
var deposits = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .toList();
```

Predykat przyjmuje `Transaction` i zwraca `boolean`.
Element przechodzi dalej tylko wtedy, gdy predykat zwróci `true`.

Typ elementu się nie zmienia:

```text
Stream<Transaction> → filter(...) → Stream<Transaction>
```

`filter`:

- może usunąć z wyniku zero lub wiele elementów,
- zachowuje kolejność źródła dla zwykłego sekwencyjnego streama listy,
- nie modyfikuje listy źródłowej,
- nie tworzy nowych transakcji — przepuszcza dalej te same obiekty.

## 2. `map`: przekształć każdy element

```java
var amounts = transactions.stream()
        .map(Transaction::amount)
        .toList();
```

`map` przyjmuje funkcję, która zwraca jeden wynik dla każdego elementu.
W tym przykładzie:

```text
Stream<Transaction> → map(...) → Stream<BigDecimal>
```

Można użyć referencji do metody:

```java
Transaction::amount
```

albo lambdy:

```java
transaction -> transaction.amount()
```

To dwie równoważne formy. `map` nie usuwa elementów: z każdego wejściowego
elementu tworzy dokładnie jeden wynik. Funkcja mapująca może oczywiście
zwrócić `null`, ale zwykle lepiej nie wprowadzać `null` do pipeline'u.

## 3. Połącz `filter` i `map`

```java
var depositAmounts = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .map(Transaction::amount)
        .toList();
```

Najpierw wybieramy wpłaty, potem z każdej wybranej transakcji pobieramy
kwotę:

```text
Transaction → filter → Transaction → map → BigDecimal
```

Kolejność jest ważna. Po `map(Transaction::amount)` strumień zawiera
`BigDecimal`, a nie `Transaction`, więc nie można już odwołać się do
`transaction.type()`.

## 4. `flatMap`: wiele kolekcji jako jeden strumień

Załóżmy, że mamy historię dla każdego rachunku:

```java
List<List<Transaction>> histories;
```

Każdy rachunek ma osobną listę transakcji. `flatMap` pozwala przejść od
strumienia list do strumienia pojedynczych transakcji:

```java
var allTransactions = histories.stream()
        .flatMap(List::stream)
        .toList();
```

Zmiana kształtu:

```text
Stream<List<Transaction>>
    flatMap(List::stream)
Stream<Transaction>
```

W tym przypadku metoda `List::stream` zamienia każdą listę transakcji
w jej strumień. `flatMap` łączy te strumienie w jeden.

Przykład:

```text
historia konta A: [T1, T2]
historia konta B: [T3]
historia konta C: []

po flatMap: [T1, T2, T3]
```

Pusta lista daje zero elementów. Nie powstaje `null`.

## 5. Różnica między `map` i `flatMap`

`map` zachowuje jedno wyjście na każdy element:

```java
var histories = accounts.stream()
        .map(Account::transactions)
        .toList();
```

Wynikiem jest lista list:

```text
List<List<Transaction>>
```

`flatMap` łączy elementy tych list:

```java
var transactions = accounts.stream()
        .flatMap(account -> account.transactions().stream())
        .toList();
```

Wynikiem jest jedna lista transakcji:

```text
List<Transaction>
```

Pomocnicza intuicja:

```text
map     : jeden element → jeden wynik
flatMap : jeden element → zero lub wiele wyników, połączonych w jeden strumień
```

`map` może zwrócić obiekt kolekcji jako pojedynczy wynik. `flatMap`
spłaszcza strumienie zwrócone przez funkcję mapującą.

## 6. Klasa do wklejenia

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\StreamTransformations.java
```

Wklej:

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamTransformations {

    public List<Transaction> filterByType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .toList();
    }

    public List<BigDecimal> amountsForType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .map(Transaction::amount)
                .toList();
    }

    public List<Transaction> flattenHistories(
            List<List<Transaction>> histories
    ) {
        Objects.requireNonNull(histories, "histories must not be null");

        for (var history : histories) {
            Objects.requireNonNull(history, "history must not be null");
            validateTransactions(history);
        }

        return histories.stream()
                .flatMap(List::stream)
                .toList();
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}
```

Walidacja wejścia jest oddzielona od pipeline'ów, aby kontrakt metody był
jawny. Dlatego `null` w zewnętrznej liście, wewnętrznej historii lub wśród
transakcji zostanie odrzucony przed rozpoczęciem przetwarzania.

## 7. Test Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\StreamTransformationsSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class StreamTransformationsSpec extends Specification {

    private final StreamTransformations transformations = new StreamTransformations()

    def "filter selects matching transactions and preserves their order"() {
        given:
        def transactions = sampleTransactions()

        when:
        def deposits = transformations.filterByType(
                transactions,
                TransactionType.DEPOSIT
        )

        then:
        deposits*.id() == ["T1", "T3"]
        deposits.every { it.type() == TransactionType.DEPOSIT }
        transactions*.id() == ["T1", "T2", "T3", "T4"]
    }

    def "map extracts one amount from each matching transaction"() {
        when:
        def amounts = transformations.amountsForType(
                sampleTransactions(),
                TransactionType.DEPOSIT
        )

        then:
        amounts == [new BigDecimal("100.00"), new BigDecimal("50.00")]
    }

    def "flatMap flattens histories preserving history and transaction order"() {
        given:
        def firstHistory = [
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00")
        ]
        def secondHistory = [
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00")
        ]

        when:
        def flattened = transformations.flattenHistories(
                [firstHistory, secondHistory]
        )

        then:
        flattened*.id() == ["T1", "T2", "T3"]
    }

    def "flatMap treats an empty history as zero transactions"() {
        given:
        def history = [
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00")
        ]

        expect:
        transformations.flattenHistories([history, [], history])*.id() ==
                ["T1", "T1"]
    }

    def "filter returns an empty list when no transaction matches"() {
        expect:
        transformations.filterByType(
                List.of(transaction(
                        "T1",
                        "PLN-1001",
                        TransactionType.DEPOSIT,
                        "100.00"
                )),
                TransactionType.TRANSFER
        ).isEmpty()
    }

    def "rejects null transaction list"() {
        when:
        transformations.filterByType(null, TransactionType.DEPOSIT)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction type"() {
        when:
        transformations.filterByType(sampleTransactions(), null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "type must not be null"
    }

    def "rejects null history"() {
        given:
        def histories = new ArrayList<List<Transaction>>()
        histories.add(null)

        when:
        transformations.flattenHistories(histories)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "history must not be null"
    }

    def "rejects null transaction inside a history"() {
        given:
        def history = new ArrayList<Transaction>()
        history.add(null)

        when:
        transformations.flattenHistories([history])

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
    }

    private static List<Transaction> sampleTransactions() {
        return List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00"),
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00"),
                transaction("T4", "PLN-1002", TransactionType.TRANSFER, "30.00")
        )
    }

    private static Transaction transaction(
            String id,
            String accountNumber,
            TransactionType type,
            String amount
    ) {
        return new Transaction(id, accountNumber, type, new BigDecimal(amount))
    }
}
```

W asercjach `*.id()` to operator spread-dot Groovy: odczytuje `id()` z
każdego elementu listy. To składnia testu Groovy, nie składnia Java Stream.

Test pustej historii celowo podaje tę samą listę dwa razy. `flatMap`
zachowuje elementy i ich kolejność; nie deduplikuje wyników.

## 8. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamTransformationsSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamTransformationsSpec"
```

Ten spec nie wymaga bazy danych ani kontekstu Springa.

## 9. Eksperyment: `map` czy `flatMap`?

Zacznij od:

```java
var nested = histories.stream()
        .map(List::stream)
        .toList();
```

Wynik nie jest `List<Transaction>`. Jest to lista strumieni:

```text
List<Stream<Transaction>>
```

Zmień `map` na `flatMap`:

```java
var flattened = histories.stream()
        .flatMap(List::stream)
        .toList();
```

Teraz otrzymujesz `List<Transaction>`. Wyjaśnij własnymi słowami, dlaczego
`map` zachowuje zagnieżdżenie, a `flatMap` łączy elementy.

## 10. Zadania do samodzielnego wykonania

Zanim spojrzysz na rozwiązanie w kodzie, spróbuj:

1. Napisać pipeline zwracający ID wszystkich wypłat.
2. Zwrócić kwoty wszystkich transakcji konta `PLN-1001`.
3. Spłaszczyć listę historii, a następnie wybrać z niej tylko wpłaty.
4. Wytłumaczyć, jaki będzie typ elementów po każdym etapie pipeline'u.

Podpowiedzi:

```text
filter po typie → zostaje Stream<Transaction>
map po ID       → powstaje Stream<String>
flatMap historii → zagnieżdżone transakcje stają się pojedynczym strumieniem
```

## Pytania kontrolne

1. Czy `filter` może zmienić typ elementu?
2. Czy `map` może usunąć element bez zwrócenia `null`?
3. Jaki jest typ wyniku `histories.stream().map(List::stream)`?
4. Co robi `flatMap` z pustym strumieniem zwróconym dla jednego elementu?
5. Czy `flatMap` usuwa duplikaty?
6. Czy `filter`, `map` albo `flatMap` zmieniają kolekcję źródłową?

Do zapamiętania:

```text
filter: Stream<T> → Stream<T>
map:    Stream<T> → Stream<R>
flatMap: wiele Stream<R> → jeden Stream<R>
```

Następna część: Lab14.3 — sortowanie, unikalność, paginowanie i wyszukiwanie.
