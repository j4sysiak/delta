# Lab14.5 — Kolektory: mapy, grupowanie i podsumowania

## Cel

W Lab14.4 redukowałeś strumień do jednej wartości. Teraz zbudujemy
bardziej złożone wyniki: mapy, grupy i podsumowania każdej grupy.

Przećwiczymy:

| Kolektor | Pytanie |
|---|---|
| `toMap` | Jak znaleźć transakcję po jej ID? |
| `groupingBy` | Jak zebrać transakcje według konta? |
| `partitioningBy` | Jak podzielić transakcje według jednego warunku? |
| `mapping` | Jak w każdej grupie zebrać tylko kwoty? |
| `teeing` | Jak dla każdej grupy obliczyć liczbę i sumę? |

Korzystamy z `Transaction` i `TransactionType` z Lab14.1. Nie zmieniamy
tych klas ani kodu produkcyjnego. Bez Springa, bazy danych i preview.
Konfigurację preview zostaw jednak dla wcześniejszych labów.

## 1. `collect(...)` i kolektor

`collect(...)` jest operacją terminalną. Przyjmuje kolektor opisujący:
jak utworzyć wynik, dodawać do niego dane, łączyć częściowe wyniki
i ewentualnie przekształcić wynik końcowy.

Nie implementujemy tego mechanizmu ręcznie. Korzystamy z gotowych
kolektorów z `java.util.stream.Collectors`.

```java
var byAccount = transactions.stream()
        .collect(Collectors.groupingBy(Transaction::accountNumber));
```

Typ wyniku:

```text
Map<String, List<Transaction>>
```

## 2. `toMap`: klucz i wartość

```java
var byId = transactions.stream()
        .collect(Collectors.toMap(
                Transaction::id,
                Function.identity()
        ));
```

Czytaj:

```text
Transaction::id      → kluczem jest ID
Function.identity() → wartością jest sama transakcja
```

`Function.identity()` odpowiada lambdzie `transaction -> transaction`.

### Konflikt kluczy

Wariant z dwoma argumentami rzuca `IllegalStateException`, jeśli dwie
transakcje mają ten sam klucz. Nawet jeśli wartości są identyczne.
Nie wybieraj przypadkowo pierwszej albo ostatniej wartości tylko po to,
aby ukryć problem danych.

Jeśli powtórzenia są zamierzone, wybierz odpowiedni kontrakt:

- `groupingBy` — zachowaj wszystkie elementy pod kluczem,
- `toMap` z funkcją łączenia — połącz wartości według jawnej reguły.

Przykład zamierzonego sumowania kwot konta:

```java
var totals = transactions.stream()
        .collect(Collectors.toMap(
                Transaction::accountNumber,
                Transaction::amount,
                BigDecimal::add
        ));
```

Trzeci argument rozwiązuje konflikt: dodaje dotychczasową i nową kwotę.

## 3. `groupingBy`: wszystkie elementy pod wspólnym kluczem

```java
var byAccount = transactions.stream()
        .collect(Collectors.groupingBy(Transaction::accountNumber));
```

Dla danych:

```text
T1: PLN-1001, 100.00
T2: PLN-1001, 20.00
T3: PLN-1002, 50.00
```

otrzymasz:

```text
PLN-1001 → [T1, T2]
PLN-1002 → [T3]
```

W odróżnieniu od `toMap` nie ma tu konfliktu: wiele elementów w jednej
grupie to oczekiwane zachowanie.

Nie zakładaj kolejności kluczy mapy ani konkretnej implementacji.
W naszym uporządkowanym, sekwencyjnym strumieniu listy wewnątrz grup
zachowują kolejność transakcji ze źródła.

## 4. `partitioningBy`: dwie grupy

```java
var partition = transactions.stream()
        .collect(Collectors.partitioningBy(
                transaction -> transaction.type() == TransactionType.DEPOSIT
        ));
```

Typ:

```text
Map<Boolean, List<Transaction>>
```

```text
true  → wpłaty
false → pozostałe operacje
```

`partitioningBy` zawsze zapewnia oba klucze, także dla pustego strumienia.
`groupingBy` tworzy tylko grupy faktycznie napotkane w danych.

## 5. `mapping`: przekształcanie wewnątrz grupy

Chcemy grupować po koncie, ale przechowywać wyłącznie kwoty:

```java
var amounts = transactions.stream()
        .collect(Collectors.groupingBy(
                Transaction::accountNumber,
                Collectors.mapping(
                        Transaction::amount,
                        Collectors.toList()
                )
        ));
```

Typ wyniku:

```text
Map<String, List<BigDecimal>>
```

Drugi argument `groupingBy` to kolektor podrzędny (downstream):
określa, co zrobić z elementami każdej grupy.

Nie możemy najpierw wykonać `map(Transaction::amount)`, a dopiero potem
grupować po koncie — po takim `map` strumień zawiera same kwoty, bez
informacji o numerze konta.

## 6. `teeing`: liczba i suma w każdej grupie

Połączymy `groupingBy` z poznanym w Lab13 `teeing`:

```text
grupa konta
├── counting() → liczba transakcji
└── reducing(...) → suma kwot
    połącz w TransactionGroupSummary
```

Sumujemy dodatnie kwoty wszystkich operacji. To suma kwot transakcji,
nie saldo konta: wypłaty i przelewy nie są tutaj odejmowane.

## 7. Rekord do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\TransactionGroupSummary.java
```

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;

public record TransactionGroupSummary(long count, BigDecimal total) {
}
```

## 8. Klasa do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\TransactionCollectors.java
```

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransactionCollectors {

    public Map<String, Transaction> byId(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.toMap(
                        Transaction::id,
                        Function.identity()
                ));
    }

    public Map<String, BigDecimal> totalsByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.toMap(
                        Transaction::accountNumber,
                        Transaction::amount,
                        BigDecimal::add
                ));
    }

    public Map<String, List<Transaction>> byAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(Transaction::accountNumber));
    }

    public Map<Boolean, List<Transaction>> depositsAndOthers(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.partitioningBy(
                        transaction -> transaction.type() == TransactionType.DEPOSIT
                ));
    }

    public Map<String, List<BigDecimal>> amountsByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,
                        Collectors.mapping(
                                Transaction::amount,
                                Collectors.toList()
                        )
                ));
    }

    public Map<String, TransactionGroupSummary> summariesByAccount(
            List<Transaction> transactions
    ) {
        validateTransactions(transactions);

        return transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::accountNumber,
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.mapping(
                                        Transaction::amount,
                                        Collectors.reducing(
                                                BigDecimal.ZERO,
                                                BigDecimal::add
                                        )
                                ),
                                TransactionGroupSummary::new
                        )
                ));
    }

    private void validateTransactions(List<Transaction> transactions) {
        Objects.requireNonNull(transactions, "transactions must not be null");

        for (var transaction : transactions) {
            Objects.requireNonNull(transaction, "transaction must not be null");
        }
    }
}
```

Nie zakładamy konkretnej klasy ani modyfikowalności zwróconych map i list.
To nie ten sam kontrakt co niemodyfikowalne `Stream.toList()` z Lab13.
Jeśli kontrakt biznesowy wymaga określonej modyfikowalności, trzeba
zaprojektować ją jawnie.

## 9. Spec Spocka do wklejenia

Plik:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\TransactionCollectorsSpec.groovy
```

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

class TransactionCollectorsSpec extends Specification {

    private final TransactionCollectors collectors = new TransactionCollectors()

    def "indexes transactions by unique id"() {
        when:
        def result = collectors.byId(sampleTransactions())

        then:
        result.size() == 4
        result.get("T2").accountNumber() == "PLN-1001"
        result.get("T2").type() == TransactionType.WITHDRAW
    }

    def "rejects duplicate ids instead of silently discarding a transaction"() {
        given:
        def transactions = List.of(
                transaction("T1", "PLN-1001", TransactionType.DEPOSIT, "100.00"),
                transaction("T1", "PLN-1002", TransactionType.DEPOSIT, "50.00")
        )

        when:
        collectors.byId(transactions)

        then:
        thrown(IllegalStateException)
    }

    def "merges repeated account keys by adding amounts"() {
        when:
        def totals = collectors.totalsByAccount(sampleTransactions())

        then:
        totals.size() == 2
        totals.get("PLN-1001") == new BigDecimal("120.00")
        totals.get("PLN-1002") == new BigDecimal("80.00")
    }

    def "groups transactions by account preserving order within each group"() {
        when:
        def groups = collectors.byAccount(sampleTransactions())

        then:
        groups.keySet() == ["PLN-1001", "PLN-1002"].toSet()
        groups.get("PLN-1001")*.id() == ["T1", "T2"]
        groups.get("PLN-1002")*.id() == ["T3", "T4"]
    }

    def "partitions deposits and other transactions"() {
        when:
        def partition = collectors.depositsAndOthers(sampleTransactions())

        then:
        partition.get(true)*.id() == ["T1", "T3"]
        partition.get(false)*.id() == ["T2", "T4"]
    }

    def "maps transactions to amounts within each account group"() {
        when:
        def amounts = collectors.amountsByAccount(sampleTransactions())

        then:
        amounts.get("PLN-1001") == [
                new BigDecimal("100.00"),
                new BigDecimal("20.00")
        ]
        amounts.get("PLN-1002") == [
                new BigDecimal("50.00"),
                new BigDecimal("30.00")
        ]
    }

    def "computes count and total for each account group"() {
        when:
        def summaries = collectors.summariesByAccount(sampleTransactions())

        then:
        summaries.size() == 2
        summaries.get("PLN-1001").count() == 2L
        summaries.get("PLN-1001").total() == new BigDecimal("120.00")
        summaries.get("PLN-1002").count() == 2L
        summaries.get("PLN-1002").total() == new BigDecimal("80.00")
    }

    def "empty input produces no account groups but both partition keys"() {
        given:
        def empty = List.of()

        expect:
        collectors.byId(empty).isEmpty()
        collectors.totalsByAccount(empty).isEmpty()
        collectors.byAccount(empty).isEmpty()
        collectors.amountsByAccount(empty).isEmpty()
        collectors.summariesByAccount(empty).isEmpty()

        when:
        def partition = collectors.depositsAndOthers(empty)

        then:
        partition.keySet() == [true, false].toSet()
        partition.get(true).isEmpty()
        partition.get(false).isEmpty()
    }

    def "rejects null input"() {
        when:
        collectors.byAccount(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
    }

    def "rejects null transaction"() {
        given:
        def transactions = new ArrayList<Transaction>()
        transactions.add(null)

        when:
        collectors.summariesByAccount(transactions)

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

`toSet()` w asercjach to metoda Groovy. Porównujemy zbiór kluczy, a nie
ich kolejność. `*.id()` to również składnia Groovy, poznana w Lab14.2.

## 10. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionCollectorsSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.TransactionCollectorsSpec"
```

Ten spec nie wymaga Springa ani bazy danych.

## 11. Eksperyment: zgubiona transakcja

Na chwilę dodaj trzeci argument do `toMap` w `byId`:

```java
.collect(Collectors.toMap(
        Transaction::id,
        Function.identity(),
        (first, second) -> first
));
```

Test konfliktu ID przestanie przechodzić: zamiast wyjątku kolektor zachowa
pierwszą transakcję i pominie drugą.

To zmiana kontraktu, nie uniwersalna naprawa. Taka polityka jest poprawna
tylko wtedy, gdy biznes jawnie wymaga zachowania pierwszej wartości.
Przywróć wersję z dwoma argumentami.

## 12. Zadanie samodzielne

Spróbuj napisać metodę zwracającą liczbę transakcji każdego typu:

```text
Map<TransactionType, Long>
```

Podpowiedź:

```java
Collectors.groupingBy(Transaction::type, Collectors.counting())
```

Dla naszych danych wynik zawiera:

```text
DEPOSIT → 2
WITHDRAW → 1
TRANSFER → 1
```

Następnie usuń przelew z danych i sprawdź, czy klucz `TRANSFER` występuje.
`groupingBy` nie tworzy automatycznie wpisu z zerem dla brakującego typu.

## Pytania kontrolne

1. Kiedy wybrać `toMap`, a kiedy `groupingBy`?
2. Co robi `toMap` z powtarzającym się kluczem bez funkcji łączenia?
3. Dlaczego funkcja łączenia jest regułą biznesową?
4. Czym `partitioningBy` różni się od `groupingBy` dla pustych danych?
5. Dlaczego używamy `mapping` wewnątrz grupowania?
6. Co zbiera każdy z dwóch kolektorów w naszym `teeing`?
7. Czy suma dodatnich kwot wszystkich operacji oznacza saldo?

Do zapamiętania:

```text
toMap → klucz i wartość, świadoma obsługa konfliktów
groupingBy → wiele elementów pod jednym kluczem
partitioningBy → dokładnie dwa klucze: true i false
mapping → przekształcenie elementów dla kolektora podrzędnego
teeing → dwa wyniki połączone w jeden
```

Następna część: Lab14.6 — efekty uboczne, `peek`, kolejność operacji
i ostrożność ze strumieniami równoległymi.
