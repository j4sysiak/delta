# Lab14.1 — Jak wykonuje się stream?

## Cel

Po tej części masz rozumieć:

- różnicę między kolekcją i strumieniem,
- źródło, operacje pośrednie i operację terminalną,
- leniwe wykonanie,
- przetwarzanie elementów przez pipeline,
- dlaczego jednego strumienia nie używa się dwa razy.

Bez wątków, Springa i bazy danych. Nie zmieniaj konfiguracji Gradle.

## 1. Kolekcja przechowuje dane, stream opisuje przetwarzanie

```java
var amounts = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .map(Transaction::amount)
        .toList();
```

Czytaj od góry:

```text
transactions.stream() → źródło: elementy listy
filter(...)           → przepuść tylko wpłaty
map(...)              → zamień transakcję na jej kwotę
toList()              → wykonaj pipeline i zbierz wynik
```

`filter` i `map` są operacjami pośrednimi. Zwracają strumienie opisujące
dalsze przetwarzanie. `toList()` jest operacją terminalną.

W tym przykładzie:

```text
Stream<Transaction>
    filter → Stream<Transaction>
    map    → Stream<BigDecimal>
    toList → List<BigDecimal>
```

`filter` nie zmienia typu elementów; `map` może go zmienić.
Żadna z tych operacji nie usuwa elementów ze źródłowej listy.

## 2. Leniwe wykonanie

Samo utworzenie pipeline'u nie uruchamia predykatu filtra:

```java
var stream = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT);
```

Dopiero operacja terminalna inicjuje przetwarzanie:

```java
var result = stream.toList();
```

Nie oznacza to, że każda operacja terminalna musi odwiedzić każdy element.
Na przykład `findFirst()` może zakończyć pracę wcześniej.
Kompilator/JDK może również pominąć niepotrzebne etapy, jeżeli nie zmienia
to wyniku zgodnie z kontraktem API. Dlatego nie opieramy logiki biznesowej
na efektach ubocznych w `map` lub `peek`.

## 3. Jeden element przechodzi przez kolejne etapy

W naszym sekwencyjnym pipeline `filter → map → toList` nie powstaje
najpierw osobna lista wszystkich przefiltrowanych transakcji.

Schemat:

```text
T1: DEPOSIT 100.00  → filter: tak → map: 100.00 → do wyniku
T2: WITHDRAW 20.00  → filter: nie
T3: DEPOSIT 50.00   → filter: tak → map: 50.00  → do wyniku
T4: TRANSFER 30.00  → filter: nie
```

Nie uogólniaj tego na wszystkie operacje: np. `sorted()` potrzebuje
zebrać elementy przed przekazaniem uporządkowanych wyników dalej.

## 4. Pliki do wklejenia

Używamy osobnego pakietu szkoleniowego. Nie importuj istniejącego modelu
transakcji z domeny banku.

### `TransactionType.java`

Ścieżka:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\TransactionType.java
```

```java
package com.delta.bank.lab.java21.streamdeepdive;

public enum TransactionType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER
}
```

### `Transaction.java`

Ścieżka:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\Transaction.java
```

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(
        String id,
        String accountNumber,
        TransactionType type,
        BigDecimal amount
) {

    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
```

Kwota jest dodatnia dla każdego typu. To typ określa rodzaj operacji;
nie zapisujemy wypłaty jako ujemnej kwoty.

### `StreamBasics.java`

Ścieżka:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\StreamBasics.java
```

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamBasics {

    public List<BigDecimal> depositAmounts(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }

        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
                .map(Transaction::amount)
                .toList();
    }
}
```

Pętla przed strumieniem jawnie waliduje wejście. Nie jest częścią pipeline'u.
Pusta lista jest poprawnym wejściem i daje pusty wynik.

## 5. Testy do wklejenia

Ścieżka:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\StreamBasicsSpec.groovy
```

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

import java.util.function.Predicate

class StreamBasicsSpec extends Specification {

    def "extracts deposit amounts without changing the source"() {
        given:
        def transactions = sampleTransactions()
        def basics = new StreamBasics()

        when:
        def amounts = basics.depositAmounts(transactions)

        then:
        amounts == [new BigDecimal("100.00"), new BigDecimal("50.00")]
        transactions.size() == 4
        transactions.get(1).type() == TransactionType.WITHDRAW
    }

    def "does not execute filter until a terminal operation is called"() {
        given:
        def visitedIds = []
        Predicate<Transaction> predicate = { Transaction transaction ->
            visitedIds.add(transaction.id())
            transaction.type() == TransactionType.DEPOSIT
        } as Predicate<Transaction>

        when:
        def stream = sampleTransactions().stream().filter(predicate)

        then:
        visitedIds.isEmpty()

        when:
        def deposits = stream.toList()

        then:
        deposits.collect { it.id() } == ["T1", "T3"]
        visitedIds == ["T1", "T2", "T3", "T4"]
    }

    def "cannot reuse a stream after a terminal operation"() {
        given:
        def stream = sampleTransactions().stream()

        when:
        def firstResult = stream.toList()

        then:
        firstResult.size() == 4

        when:
        stream.toList()

        then:
        thrown(IllegalStateException)
    }

    def "can create a new stream from the same list"() {
        given:
        def transactions = sampleTransactions()

        when:
        def firstResult = transactions.stream().toList()
        def secondResult = transactions.stream().toList()

        then:
        firstResult == secondResult
    }

    def "returns an empty result for an empty source"() {
        expect:
        new StreamBasics().depositAmounts(List.of()).isEmpty()
    }

    def "rejects a null source"() {
        when:
        new StreamBasics().depositAmounts(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects a null transaction"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        new StreamBasics().depositAmounts(transactions)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transaction must not be null"
    }

    private static List<Transaction> sampleTransactions() {
        return List.of(
                new Transaction("T1", "PLN-1001", TransactionType.DEPOSIT,
                        new BigDecimal("100.00")),
                new Transaction("T2", "PLN-1001", TransactionType.WITHDRAW,
                        new BigDecimal("20.00")),
                new Transaction("T3", "PLN-1002", TransactionType.DEPOSIT,
                        new BigDecimal("50.00")),
                new Transaction("T4", "PLN-1002", TransactionType.TRANSFER,
                        new BigDecimal("30.00"))
        )
    }
}
```

W drugim teście zapisujemy odwiedzone identyfikatory wyłącznie po to,
aby zobaczyć wykonanie sekwencyjnego filtra. To instrumentacja testowa,
nie wzorzec do dodawania efektów ubocznych do kodu biznesowego.

`as Predicate<Transaction>` jawnie określa interfejs, do którego Groovy
ma dopasować closure.

`deposits.collect { it.id() }` to metoda kolekcji Groovy, nie Java
`Stream.collect(...)`. Tutaj tylko ułatwia zapis oczekiwania w teście.

## 6. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamBasicsSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamBasicsSpec"
```

## 7. Eksperyment: wcześniejsze zakończenie

W teście leniwego wykonania zmień tylko drugi blok `when` i odpowiadający
mu blok `then`:

```groovy
when:
def firstDeposit = stream.findFirst()

then:
firstDeposit.isPresent()
firstDeposit.get().id() == "T1"
visitedIds == ["T1"]
```

Dlaczego odwiedziliśmy tylko T1? Pierwszy element spełnia filtr, a
`findFirst()` nie potrzebuje kolejnych wyników.

Teraz zmień warunek predykatu z `DEPOSIT` na `WITHDRAW` i oczekiwania na:

```groovy
firstDeposit.get().id() == "T2"
visitedIds == ["T1", "T2"]
```

Zmienna ma wtedy historyczną nazwę `firstDeposit`, ale wynik jest wypłatą.
Możesz nazwać ją `firstMatch`. Po eksperymencie przywróć początkowy test.

## 8. Pytania kontrolne

1. Co jest źródłem naszego streama?
2. Czy samo `filter(...)` uruchamia predykat?
3. Jaki typ elementów mamy przed `map`, a jaki po nim?
4. Czy pipeline zmienia listę źródłową?
5. Dlaczego drugi `toList()` na tym samym streamie zgłasza błąd?
6. Dlaczego `findFirst()` może odwiedzić mniej elementów niż `toList()`?

Do zapamiętania:

```text
Kolekcja przechowuje elementy.
Stream opisuje ich przetwarzanie.
Operacja terminalna inicjuje wykonanie.
Jedna instancja streama służy do jednego użycia.
```

Następna część: Lab14.2 — `filter`, `map` i `flatMap`.
