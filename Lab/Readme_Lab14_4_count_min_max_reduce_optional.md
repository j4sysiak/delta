# Lab14.4 — `count`, `min`, `max`, `reduce` i `Optional`

## Cel

W tej części przećwiczysz operacje terminalne, które odpowiadają na pytania
o całym strumieniu:

- ile jest elementów? — `count()`,
- jaka jest najmniejsza lub największa wartość? — `min(...)`, `max(...)`,
- jaka jest suma? — `reduce(...)`,
- co zrobić, gdy strumień jest pusty? — `Optional`.

Korzystamy z `Transaction` i `TransactionType` z Lab14.1. Nie twórz tych
klas ponownie i nie zmieniaj produkcyjnego mini-banku.

## 1. `count()`

```java
long numberOfTransactions = transactions.stream().count();
```

`count()` jest operacją terminalną i zwraca `long`, nie `int`.
Nie musisz wcześniej mapować elementów do czegoś, co da się policzyć.

## 2. `min(...)` i `max(...)`

```java
Optional<BigDecimal> smallest = transactions.stream()
        .map(Transaction::amount)
        .min(BigDecimal::compareTo);
```

`min` i `max` potrzebują `Comparator`, który określa porządek.
Wynikiem jest `Optional<BigDecimal>`, ponieważ pusty strumień nie ma
minimum ani maksimum.

Można bezpiecznie obsłużyć brak wyniku:

```java
String description = smallest
        .map(BigDecimal::toPlainString)
        .orElse("no transactions");
```

Nie wywołuj `get()` bez sprawdzenia, czy `Optional` zawiera wartość.
W produkcyjnym kodzie lepiej wybrać świadome zachowanie: wartość domyślną,
inny typ wyniku albo jawny wyjątek, zgodnie z kontraktem metody.

`BigDecimal::compareTo` porównuje wartości numerycznie, bez rozróżniania
skali. Jeśli kilka kwot ma taką samą wartość liczbową, `min`/`max` nie
stanowi mechanizmu normalizacji skali. W tym labie porównujemy wartości,
a nie formatowanie.

## 3. `reduce`: złącz elementy w jeden wynik

### Wariant z wartością początkową (identity)

```java
BigDecimal total = amounts.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

Ten wariant zwraca bezpośrednio `BigDecimal`. Dla pustego strumienia wynik
to `BigDecimal.ZERO`.

`BigDecimal.ZERO` jest identity dla dodawania:

```text
0 + x = x
x + 0 = x
```

### Wariant bez identity

```java
Optional<BigDecimal> total = amounts.stream()
        .reduce(BigDecimal::add);
```

Bez wartości początkowej wynik jest `Optional<BigDecimal>`:

- pusty strumień daje `Optional.empty()`,
- niepusty strumień daje sumę elementów.

### Kiedy `reduce` jest właściwym narzędziem?

`reduce` służy do łączenia elementów w jeden wynik, na przykład sumę,
iloczyn albo maksimum. Operator łączenia powinien być łączny
(associative), a identity powinno być prawidłową wartością neutralną.
Jest to szczególnie ważne dla poprawności redukcji równoległej.

Nie używaj `reduce` do zbierania elementów przez mutowanie współdzielonej
listy. Do tego służą kolektory, które będą tematem Lab14.5.

## 4. Klasa do wklejenia

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\TransactionStatistics.java
```

Wklej:

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TransactionStatistics {

    public long count(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .count();
    }

    public Optional<BigDecimal> minimumAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .min(BigDecimal::compareTo);
    }

    public Optional<BigDecimal> maximumAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .max(BigDecimal::compareTo);
    }

    public BigDecimal totalAmount(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Optional<BigDecimal> totalAmountIfPresent(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal::add);
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}
```

Metody `totalAmount` i `totalAmountIfPresent` sumują te same dane, ale mają
różny kontrakt dla pustej listy:

```text
totalAmount([])          → 0
totalAmountIfPresent([]) → Optional.empty()
```

Wybór zależy od znaczenia pustego wejścia. Jeśli brak transakcji oznacza
sumę równą zero, wariant z identity jest wygodny. Jeśli brak danych ma
pozostać odróżnialny od sumy zero, zwróć `Optional`.

## 5. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\TransactionStatisticsSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionStatisticsSpec extends Specification {

    private final TransactionStatistics statistics = new TransactionStatistics()

    def "counts all transactions"() {
        expect:
        statistics.count(sampleTransactions()) == 5L
    }

    def "finds minimum and maximum amount"() {
        when:
        def minimum = statistics.minimumAmount(sampleTransactions())
        def maximum = statistics.maximumAmount(sampleTransactions())

        then:
        minimum.isPresent()
        minimum.get().compareTo(new BigDecimal("20.00")) == 0
        maximum.isPresent()
        maximum.get().compareTo(new BigDecimal("100.00")) == 0
    }

    def "sums amounts using reduce with identity"() {
        expect:
        statistics.totalAmount(sampleTransactions()) ==
                new BigDecimal("300.00")
    }

    def "sums amounts using reduce without identity"() {
        when:
        def total = statistics.totalAmountIfPresent(sampleTransactions())

        then:
        total.isPresent()
        total.get().compareTo(new BigDecimal("300.00")) == 0
    }

    def "empty stream has a count of zero and no minimum or maximum"() {
        given:
        def empty = List.<Transaction>of()

        expect:
        statistics.count(empty) == 0L
        statistics.minimumAmount(empty).isEmpty()
        statistics.maximumAmount(empty).isEmpty()
    }

    def "identity reduction returns zero for an empty stream"() {
        expect:
        statistics.totalAmount(List.<Transaction>of()) == BigDecimal.ZERO
    }

    def "reduction without identity returns an empty Optional"() {
        expect:
        statistics.totalAmountIfPresent(List.<Transaction>of()).isEmpty()
    }

    def "rejects null transaction list"() {
        when:
        statistics.count(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction in list"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        statistics.totalAmount(transactions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
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

W testach minimum i maksimum porównujemy przez `compareTo`, bo sprawdzamy
wartość numeryczną, a nie skalę `BigDecimal`.

`List.<Transaction>of()` jawnie podaje typ elementu pustej listy. Dzięki
temu Java poprawnie dopasowuje generyczny typ wejściowy metody.

## 6. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionStatisticsSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionStatisticsSpec"
```

Ten spec nie wymaga bazy danych ani kontekstu Springa.

## 7. Eksperymenty

### Eksperyment A: dodawanie nie musi zwracać `Optional`

W specu porównaj wyniki dla pustej listy:

```text
reduce(BigDecimal.ZERO, BigDecimal::add) → BigDecimal.ZERO
reduce(BigDecimal::add)                 → Optional.empty()
```

Wyjaśnij, jaka informacja zostaje zachowana w drugim wariancie, a jaka
zostaje sprowadzona do zera w pierwszym.

### Eksperyment B: `min` i `max` nie sortują całej listy

`min(...)` i `max(...)` zwracają tylko jedną wartość. Nie dostajesz
posortowanej kolekcji ani nie zmieniasz kolejności źródła. Jeśli potrzebujesz
wszystkich elementów w kolejności, użyj `sorted(...)` z Lab14.3.

## 8. Pytania kontrolne

1. Dlaczego `count()` zwraca `long`?
2. Dlaczego `min()` i `max()` zwracają `Optional`?
3. Jaka jest różnica pomiędzy `reduce(identity, accumulator)` a
   `reduce(accumulator)`?
4. Dlaczego `BigDecimal.ZERO` jest identity dla dodawania?
5. Co oznacza, że operator redukcji jest łączny?
6. Dlaczego nie należy mutować współdzielonej listy wewnątrz `reduce`?
7. Czy `min()` sortuje cały strumień?

Do zapamiętania:

```text
count → long
min/max → Optional<T>, bo strumień może być pusty
reduce(identity, op) → wynik także dla pustego strumienia
reduce(op) → Optional<T>, pusty strumień daje Optional.empty()
```

Następna część: Lab14.5 — kolektory mapujące i grupujące.
