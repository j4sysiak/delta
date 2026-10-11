# Lab14.3 — Sortowanie, unikalność, wybór i wyszukiwanie

## Cel

W tej części poznasz:

- `sorted(...)` — porządkowanie elementów,
- `distinct()` — usuwanie duplikatów według `equals()`,
- `skip(...)` i `limit(...)` — wybieranie fragmentu strumienia,
- `findFirst()` — znalezienie pierwszego pasującego elementu,
- `anyMatch`, `allMatch`, `noneMatch` — sprawdzanie warunków.

Korzystamy z `Transaction` i `TransactionType` z Lab14.1. Nie twórz ich
ponownie i nie zmieniaj kodu produkcyjnego mini-banku.

## 1. `sorted`: porządkowanie

```java
var sorted = transactions.stream()
        .sorted(Comparator.comparing(Transaction::amount))
        .toList();
```

`sorted` nie zmienia elementów ani ich typu; ustala ich kolejność.
Porządkowanie jest stabilne: elementy uznane przez comparator za równe
zachowują względem siebie kolejność ze źródła.

W praktycznym kodzie comparator warto doprecyzować drugim kryterium:

```java
var sorted = transactions.stream()
        .sorted(
                Comparator.comparing(Transaction::amount)
                        .thenComparing(Transaction::id)
        )
        .toList();
```

Wynik będzie jednoznaczny także dla transakcji o tej samej kwocie.
Porządek rosnący oznacza kwoty od najmniejszej do największej.

`sorted()` bez argumentu wymaga, aby elementy implementowały
`Comparable`. Nasz rekord `Transaction` tego nie robi, więc podajemy
comparator.

## 2. `distinct`: duplikat według `equals()`

```java
var uniqueAmounts = transactions.stream()
        .map(Transaction::amount)
        .distinct()
        .toList();
```

`distinct()` porównuje elementy zgodnie z `equals()` i `hashCode()`.
Dla rekordu `Transaction` porównywane są wszystkie składowe rekordu.
Jeśli wcześniej mapujemy do kwot, duplikaty określa `BigDecimal.equals()`.

To ważne, bo `BigDecimal.equals()` uwzględnia skalę:

```java
new BigDecimal("10.0").equals(new BigDecimal("10.00"))       // false
new BigDecimal("10.0").compareTo(new BigDecimal("10.00")) == 0 // true
```

W rezultacie `distinct()` zachowa obie kwoty `10.0` i `10.00`.
`distinct()` nie oznacza automatycznie „usuń wartości o tej samej wartości
numerycznej”.

## 3. `skip` i `limit`: fragment strumienia

```java
var page = transactions.stream()
        .skip(2)
        .limit(3)
        .toList();
```

Dla uporządkowanego źródła, takiego jak lista:

- `skip(n)` pomija pierwsze `n` elementów,
- `limit(n)` przepuszcza najwyżej `n` elementów,
- razem mogą utworzyć prostą stronę wyników.

Kolejność ma znaczenie:

```java
stream.skip(2).limit(3) // pomiń 2, potem weź do 3
stream.limit(3).skip(2) // weź do 3, potem pomiń 2
```

Wyniki są różne. Paginacja zwykle wymaga stabilnego sortowania przed
`skip`/`limit`, inaczej pozycje elementów mogą się zmieniać między
wywołaniami.

## 4. Wyszukiwanie i dopasowanie warunków

### `findFirst()`

```java
Optional<Transaction> firstDeposit = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .findFirst();
```

`findFirst()` zwraca `Optional<Transaction>`, bo może nie być dopasowania.
Nie zwraca `null`.

W tym labie używamy strumieni sekwencyjnych. Przy strumieniu równoległym
`findFirst()` nadal respektuje kolejność spotkania, podczas gdy
`findAny()` może zwrócić dowolny pasujący element.

### Match operations

```java
boolean hasLargeDeposit = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .anyMatch(transaction -> transaction.amount().compareTo(limit) >= 0);
```

- `anyMatch(predicate)` — czy przynajmniej jeden element pasuje?
- `allMatch(predicate)` — czy wszystkie elementy pasują?
- `noneMatch(predicate)` — czy żaden element nie pasuje?

To operacje terminalne i mogą zakończyć przetwarzanie wcześniej, gdy wynik
jest już znany.

Dla pustego strumienia:

```text
anyMatch(...)  → false
allMatch(...)  → true
noneMatch(...) → true
```

To logiczne przypadki brzegowe: nie istnieje element, który obaliłby
`allMatch` albo spełnił `anyMatch`.

## 5. Klasa do wklejenia

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\TransactionQueries.java
```

Wklej:

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransactionQueries {

    public List<Transaction> sortedByAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .sorted(
                        Comparator.comparing(Transaction::amount)
                                .thenComparing(Transaction::id)
                )
                .toList();
    }

    public List<BigDecimal> distinctAmounts(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .distinct()
                .toList();
    }

    public List<Transaction> page(
            List<Transaction> transactions,
            int pageNumber,
            int pageSize
    ) {
        validateTransactions(transactions);

        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        long offset = (long) pageNumber * pageSize;

        return transactions.stream()
                .skip(offset)
                .limit(pageSize)
                .toList();
    }

    public Optional<Transaction> firstByType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .filter(transaction -> transaction.type() == type)
                .findFirst();
    }

    public boolean anyAmountAtLeast(
            List<Transaction> transactions,
            BigDecimal minimum
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(minimum, "minimum must not be null");

        return transactions.stream()
                .anyMatch(transaction ->
                        transaction.amount().compareTo(minimum) >= 0
                );
    }

    public boolean allForAccount(
            List<Transaction> transactions,
            String accountNumber
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");

        return transactions.stream()
                .allMatch(transaction ->
                        transaction.accountNumber().equals(accountNumber)
                );
    }

    public boolean noneOfType(
            List<Transaction> transactions,
            TransactionType type
    ) {
        validateTransactions(transactions);
        Objects.requireNonNull(type, "type must not be null");

        return transactions.stream()
                .noneMatch(transaction -> transaction.type() == type);
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}
```

`page(...)` zachowuje kolejność wejściowej listy, ale sam jej nie sortuje.
Jeżeli strona ma mieć stabilne znaczenie, wywołujący powinien najpierw
ustalić kolejność, np. przez `sortedByAmount(...)`.

Mnożenie offsetu przez `long` zapobiega przepełnieniu `int` przy
obliczaniu przesunięcia strony.

## 6. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\TransactionQueriesSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionQueriesSpec extends Specification {

    private final TransactionQueries queries = new TransactionQueries()

    def "sorts by amount and then transaction id"() {
        when:
        def sorted = queries.sortedByAmount(sampleTransactions())

        then:
        sorted*.id() == ["T2", "T4", "T3", "T1", "T5"]
        sorted*.amount() == [
                new BigDecimal("20.00"),
                new BigDecimal("30.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        ]
    }

    def "distinct uses BigDecimal equals and therefore considers scale"() {
        when:
        def amounts = queries.distinctAmounts(sampleTransactions())

        then:
        amounts == [
                new BigDecimal("100.00"),
                new BigDecimal("20.00"),
                new BigDecimal("50.00"),
                new BigDecimal("30.00")
        ]
    }

    def "paginates in source order using zero-based page number"() {
        when:
        def page = queries.page(sampleTransactions(), 1, 2)

        then:
        page*.id() == ["T3", "T4"]
    }

    def "returns a shorter last page when there are fewer remaining elements"() {
        when:
        def page = queries.page(sampleTransactions(), 2, 2)

        then:
        page*.id() == ["T5"]
    }

    def "returns an empty page when offset is past the end"() {
        expect:
        queries.page(sampleTransactions(), 10, 2).isEmpty()
    }

    def "findFirst returns the first matching transaction in encounter order"() {
        when:
        def result = queries.firstByType(
                sampleTransactions(),
                TransactionType.DEPOSIT
        )

        then:
        result.isPresent()
        result.get().id() == "T1"
    }

    def "findFirst returns an empty Optional when no transaction matches"() {
        when:
        def result = queries.firstByType(
                sampleTransactions(),
                TransactionType.TRANSFER
        )

        then:
        !result.isPresent()
    }

    def "anyMatch is true when at least one transaction meets the condition"() {
        expect:
        queries.anyAmountAtLeast(
                sampleTransactions(),
                new BigDecimal("100.00")
        )
    }

    def "allMatch is false when one transaction belongs to a different account"() {
        expect:
        !queries.allForAccount(sampleTransactions(), "PLN-1001")
    }

    def "noneMatch is true when no transaction has the requested type"() {
        expect:
        queries.noneOfType(sampleTransactions(), TransactionType.TRANSFER)
    }

    def "match operations use their defined results for an empty list"() {
        expect:
        !queries.anyAmountAtLeast(List.of(), BigDecimal.ONE)
        queries.allForAccount(List.of(), "PLN-1001")
        queries.noneOfType(List.of(), TransactionType.DEPOSIT)
    }

    def "rejects a negative page number"() {
        when:
        queries.page(sampleTransactions(), -1, 2)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "pageNumber must not be negative"
    }

    def "rejects a non-positive page size"() {
        when:
        queries.page(sampleTransactions(), 0, 0)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "pageSize must be positive"
    }

    private static List<Transaction> sampleTransactions() {
        return List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T2", "PLN-1001", TransactionType.WITHDRAW, "20.00"),
                transaction("T3", "PLN-1002", TransactionType.DEPOSIT, "50.00"),
                transaction("T4", "PLN-1002", TransactionType.WITHDRAW, "30.00"),
                transaction("T5", "PLN-1001", TransactionType.DEPOSIT, "100.00")
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

W danych testowych obie wpłaty po `100.00` są identyczne kwotowo, ale to
różne transakcje. Sortowanie rozstrzyga ich kolejność przez ID. `distinct`
kwot zachowuje pierwsze wystąpienie każdej wartości `BigDecimal.equals()`.

## 7. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionQueriesSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionQueriesSpec"
```

Ten spec nie wymaga bazy danych ani kontekstu Springa.

## 8. Eksperyment: `BigDecimal.equals()` kontra `compareTo()`

W tym eksperymencie używaj kwot:

```java
new BigDecimal("10.0")
new BigDecimal("10.00")
new BigDecimal("10.0")
```

Przewidź wynik:

```java
amounts.stream().distinct().toList()
```

`distinct()` zostawi `10.0` i `10.00`, ale usunie drugie `10.0`, ponieważ
opiera się na `equals()`.

Sprawdź różnicę:

```java
new BigDecimal("10.0").equals(new BigDecimal("10.00"))        // false
new BigDecimal("10.0").compareTo(new BigDecimal("10.00")) == 0 // true
```

Nie próbuj „naprawiać” tego przez zaokrąglanie kwot bez określonej reguły
domenowej. Jeśli biznes wymaga unikalności numerycznej niezależnej od
skali, trzeba jawnie zdefiniować normalizację albo comparator.

## 9. Eksperyment: kolejność paginacji

Dla listy:

```text
T1, T2, T3, T4, T5
```

porównaj:

```java
stream.skip(2).limit(2).toList() // T3, T4
stream.limit(2).skip(1).toList() // T2
```

Każda operacja działa na wyniku poprzedniej. Dlatego `skip` i `limit`
nie są zamienne.

## 10. Pytania kontrolne

1. Według jakiego kontraktu `distinct()` rozpoznaje duplikaty?
2. Dlaczego `BigDecimal("10.0")` i `BigDecimal("10.00")` mogą przejść
   przez `distinct()` jako osobne wartości?
3. Dlaczego strona powinna opierać się na stabilnym sortowaniu?
4. Jaka jest różnica między `findFirst()` i `findAny()`?
5. Co zwraca `findFirst()`, jeśli nie ma dopasowania?
6. Jakie wyniki dają `anyMatch`, `allMatch` i `noneMatch` dla pustego
   strumienia?
7. Czy operacje match muszą odwiedzić wszystkie elementy?

Do zapamiętania:

```text
sorted      → ustala kolejność
distinct    → używa equals/hashCode
skip/limit  → wybiera fragment w kolejności spotkania
findFirst   → Optional z pierwszym dopasowaniem
any/all/noneMatch → odpowiedź logiczna, często z wcześniejszym zakończeniem
```

Następna część: Lab14.4 — `count`, `min`, `max`, `reduce` i `Optional`.
