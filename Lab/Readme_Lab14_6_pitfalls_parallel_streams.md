# Lab14.6 — Pułapki, efekty uboczne i strumienie równoległe

## Cel

To ostatnia część cyklu Lab 14. Uporządkujemy zasady bezpiecznego pisania
pipeline'ów i sprawdzimy, co zmienia `parallelStream()`.

Poznasz:

- dlaczego efekty uboczne w operacjach pośrednich są ryzykowne,
- jak używać `peek` wyłącznie pomocniczo,
- dlaczego kolejność operacji ma znaczenie,
- różnicę między kolejnością spotkania a kolejnością wykonania,
- bezpieczne sumowanie równoległe przez `reduce`,
- dlaczego `forEach` i współdzielona mutowalna lista są problematyczne.

Korzystamy z `Transaction` i `TransactionType` z Lab14.1. Nie modyfikujemy
tych klas ani kodu produkcyjnego mini-banku. Ćwiczenie nie potrzebuje
Springa, bazy ani preview.

## 1. Operacje pośrednie nie są miejscem na efekty uboczne

Unikaj kodu w tym stylu:

```java
var processed = new ArrayList<Transaction>();

transactions.stream()
        .filter(transaction -> {
            processed.add(transaction);
            return transaction.type() == TransactionType.DEPOSIT;
        })
        .toList();
```

Predykat filtra powinien odpowiadać na pytanie „czy element przechodzi
dalej?”, a nie zmieniać zewnętrzny stan. Efekty uboczne utrudniają
rozumienie i testowanie pipeline'u, a w strumieniu równoległym mogą
prowadzić do wyścigów.

Nie zakładaj też, że każda operacja pośrednia na pewno wykona się dla
każdego elementu. Implementacja może pominąć etapy, jeśli wynik da się
obliczyć bez ich wykonania, a nie zmieni to wyniku zgodnie z kontraktem
Stream API. Przykładowo `count()` może wykorzystać znany rozmiar źródła
i pominąć etapy, których wynik nie wpływa na liczbę elementów.

Dlatego taki kod jest błędny jako sposób liczenia wywołań:

```java
long[] calls = {0};
long count = transactions.stream()
        .map(transaction -> {
            calls[0]++;
            return transaction;
        })
        .count();
```

Nie traktuj `calls[0]` jako wiarygodnego licznika. Nie opieraj poprawności
programu na tym, że ta funkcja mapująca zostanie wywołana.

## 2. `peek`: podgląd, nie transformacja

`peek` pozwala wykonać akcję dla elementów, które przechodzą przez etap:

```java
var deposits = transactions.stream()
        .peek(transaction -> logger.debug("Found {}", transaction.id()))
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .toList();
```

Może być przydatny przy krótkiej diagnostyce, gdy pipeline ma operację
terminalną, która faktycznie konsumuje elementy. Nie używaj go do:

- aktualizowania danych domenowych,
- zapisywania wyników do zewnętrznej kolekcji,
- wykonywania obowiązkowych zapisów,
- sterowania logiką biznesową.

Akcja `peek` może nie zostać wykonana dla każdego elementu, jeśli
implementacja może pominąć ten etap bez wpływu na wynik. Usuń tymczasowe
logowanie po diagnozie albo zastąp je jawnym kodem, jeśli działanie jest
wymagane przez kontrakt.

## 3. Kolejność operacji wpływa na wynik i koszt

Te pipeline'y nie są równoważne:

```java
transactions.stream()
        .filter(this::isDeposit)
        .map(this::expensiveTransformation)
        .toList();
```

```java
transactions.stream()
        .map(this::expensiveTransformation)
        .filter(this::isDepositResult)
        .toList();
```

Druga wersja może przekształcać elementy, które pierwsza odrzuciłaby
wcześniej. Może też zmienić znaczenie, jeśli predykaty działają na
różnych typach. Jeśli semantyka na to pozwala, filtr przed kosztownym
przekształceniem zwykle ogranicza liczbę wykonań mapowania.

Kolejność można mierzyć deterministycznie na sekwencyjnym streamie
z instrumentacją testową. Nie używaj takiej instrumentacji jako logiki
produkcyjnej:

```java
long[] mapped = {0};

var result = transactions.stream()
        .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
        .map(transaction -> {
            mapped[0]++;
            return transaction.id();
        })
        .toList();
```

Przy terminalnym `toList()` na zwykłym, sekwencyjnym streamie lista `mapped`
pokazuje liczbę elementów, które przeszły przez mapper. W kodzie biznesowym
mapper powinien jednak pozostać bez efektów ubocznych.

## 4. Kolejność spotkania i kolejność wykonania

Lista jest uporządkowanym źródłem. Jej kolejność spotkania (encounter
order) to kolejność elementów podczas iteracji.

`parallelStream()` może wykonywać pracę równolegle, ale nie oznacza to
automatycznie utraty kolejności spotkania. Dla uporządkowanego źródła:

```java
var ids = transactions.parallelStream()
        .map(Transaction::id)
        .toList();
```

`toList()` zachowuje kolejność spotkania. Natomiast akcje w `forEach`
wykonują się w kolejności, której nie należy zakładać:

```java
transactions.parallelStream()
        .forEach(transaction -> logger.info(transaction.id()));
```

Jeśli dla uporządkowanego streama potrzebujesz uporządkowanego efektu
`forEach`, istnieje `forEachOrdered(...)`, ale ograniczenie kolejności
może zmniejszyć korzyści z równoległości.

Kolejność spotkania to nie kolejność, w jakiej wątki wykonują poszczególne
etapy. Stream równoległy może przetwarzać elementy w różnej kolejności,
a mimo to zwrócić listę w kolejności źródła.

## 5. Bezpieczne i niebezpieczne sumowanie

### Bezpieczna redukcja

```java
BigDecimal total = transactions.parallelStream()
        .map(Transaction::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

`BigDecimal` jest niemutowalny, a dodawanie jest łączne i ma identity
`BigDecimal.ZERO`. Dzięki temu redukcja może łączyć częściowe wyniki.

### Niebezpieczna mutacja współdzielonego akumulatora

Nie rób tak:

```java
var total = new BigDecimal[] { BigDecimal.ZERO };

transactions.parallelStream().forEach(transaction ->
        total[0] = total[0].add(transaction.amount())
);
```

Wiele wątków równocześnie odczytuje i zapisuje tę samą komórkę. Wynik
może być błędny, a zachowanie nie jest bezpieczne wątkowo.

Unikaj również współdzielonej listy:

```java
var result = new ArrayList<String>();

transactions.parallelStream()
        .forEach(transaction -> result.add(transaction.id()));
```

`ArrayList` nie jest bezpieczna do równoczesnego zapisu. Jeśli chcesz
zebrać wyniki strumienia, użyj kolektora:

```java
var result = transactions.parallelStream()
        .map(Transaction::id)
        .toList();
```

Kolektory standardowe zarządzają zbieraniem częściowych wyników.
Nie oznacza to, że każdy własny kolektor jest bezpieczny ani że parallel
zawsze będzie szybszy.

## 6. Kiedy w ogóle rozważyć `parallelStream()`?

Równoległość nie jest darmowa. Dochodzą koszty podziału pracy, koordynacji
i łączenia wyników. Wynik zależy m.in. od:

- liczby i kosztu elementów,
- możliwości podziału źródła,
- kosztu operacji,
- dostępnych zasobów,
- tego, czy operacje są bezpieczne wątkowo,
- zachowania zewnętrznych usług lub baz danych.

Nie wybieraj streama równoległego tylko dlatego, że jest krótszy albo
„ma więcej wątków”. Zmierz rzeczywisty przypadek w odpowiednim
środowisku i sprawdź, czy semantyka oraz ograniczenia aplikacji na to
pozwalają. W tym labie używamy równoległości wyłącznie do nauki.

## 7. Klasa do wklejenia

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\streamdeepdive\StreamSafetyExamples.java
```

Wklej:

```java
package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class StreamSafetyExamples {

    public List<String> depositIds(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .filter(transaction -> transaction.type() == TransactionType.DEPOSIT)
                .map(Transaction::id)
                .toList();
    }

    public BigDecimal totalSequential(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalParallel(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.parallelStream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<String> idsInEncounterOrder(List<Transaction> transactions) {
        validateTransactions(transactions);

        return transactions.parallelStream()
                .map(Transaction::id)
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

Metoda `totalParallel` jest tu dla porównania i nauki. Nie zakładamy,
że w prawdziwej aplikacji będzie szybsza od `totalSequential`.

## 8. Spec Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\streamdeepdive\StreamSafetyExamplesSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.streamdeepdive

import spock.lang.Specification

import java.util.function.Function

class StreamSafetyExamplesSpec extends Specification {

    private final StreamSafetyExamples examples = new StreamSafetyExamples()

    def "filters before mapping and transforms only matching elements"() {
        given:
        def transactions = sampleTransactions()
        def mappedIds = []
        Function<Transaction, String> idMapper = { Transaction transaction ->
            mappedIds.add(transaction.id())
            transaction.id()
        } as Function<Transaction, String>

        when:
        def ids = transactions.stream()
                .filter { Transaction transaction ->
                    transaction.type() == TransactionType.DEPOSIT
                }
                .map(idMapper)
                .toList()

        then:
        ids == ["T1", "T3"]
        mappedIds == ["T1", "T3"]
    }

    def "does not use stream operations to mutate the source"() {
        given:
        def transactions = sampleTransactions()
        def originalIds = transactions*.id()

        when:
        def depositIds = examples.depositIds(transactions)

        then:
        depositIds == ["T1", "T3"]
        transactions*.id() == originalIds
    }

    def "sequential and parallel reductions produce the same total"() {
        when:
        def sequentialTotal = examples.totalSequential(sampleTransactions())
        def parallelTotal = examples.totalParallel(sampleTransactions())

        then:
        sequentialTotal == new BigDecimal("200.00")
        parallelTotal == sequentialTotal
    }

    def "parallel toList preserves encounter order for a list source"() {
        given:
        def transactions = sampleTransactions()

        expect:
        examples.idsInEncounterOrder(transactions) == ["T1", "T2", "T3", "T4"]
    }

    def "returns empty results and zero for an empty input"() {
        given:
        def empty = List.<Transaction>of()

        expect:
        examples.depositIds(empty).isEmpty()
        examples.totalSequential(empty) == BigDecimal.ZERO
        examples.totalParallel(empty) == BigDecimal.ZERO
        examples.idsInEncounterOrder(empty).isEmpty()
    }

    def "rejects null input"() {
        when:
        examples.totalParallel(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "transactions must not be null"
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

Pierwszy test używa instrumentacji w teście, żeby policzyć wywołania
mappera dla sekwencyjnego streama zakończonego `toList()`. To nie jest
wzorzec do kopiowania do kodu biznesowego.

Nie pisz testu oczekującego konkretnej kolejności callbacków
`parallelStream().forEach(...)`. Taka kolejność nie jest gwarantowana.

## 9. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamSafetyExamplesSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streamdeepdive.StreamSafetyExamplesSpec"
```

Ten spec nie wymaga Springa ani bazy danych.

## 10. Eksperyment: `forEach` nie gwarantuje kolejności callbacków

Uruchom jako samodzielny eksperyment (nie jako test z asercją kolejności):

```java
transactions.parallelStream()
        .forEach(transaction -> System.out.println(transaction.id()));
```

Powtórz kilka razy. Możesz zobaczyć różną kolejność, ale przy małej liście
wyniki mogą też przypadkiem wyglądać tak samo. Brak zaobserwowanej zmiany
nie jest gwarancją kolejności.

Dla uporządkowanego wyniku użyj:

```java
var ids = transactions.parallelStream()
        .map(Transaction::id)
        .toList();
```

Jeśli potrzebne jest wykonanie efektu ubocznego w kolejności spotkania:

```java
transactions.parallelStream()
        .forEachOrdered(transaction -> System.out.println(transaction.id()));
```

Efekty uboczne nadal powinny być minimalne, a ograniczenie kolejności może
zmniejszyć równoległość.

## 11. Eksperyment: filtruj przed kosztownym mapowaniem

Rozważ dane, w których tylko część transakcji jest wpłatami. Porównaj:

```java
stream.filter(this::isDeposit)
        .map(this::expensiveTransformation)
        .toList();
```

z:

```java
stream.map(this::expensiveTransformation)
        .filter(this::isDepositResult)
        .toList();
```

Jeśli wynik i semantyka są równoważne, pierwsza wersja zwykle wykonuje
kosztowne mapowanie dla mniejszej liczby elementów. Nie przenoś operacji
przez pipeline, jeśli zmienia to znaczenie programu.

## 12. Pytania kontrolne

1. Dlaczego nie wolno polegać na efekcie ubocznym w `map` przed `count()`?
2. Do czego służy `peek`, a do czego nie powinno się go używać?
3. Czy stream równoległy zawsze traci kolejność spotkania?
4. Czy `parallelStream().forEach(...)` gwarantuje kolejność callbacków?
5. Dlaczego `parallelStream().map(...).toList()` może zachować kolejność?
6. Co sprawia, że redukcja `BigDecimal.ZERO` i `BigDecimal::add` jest
   bezpieczna dla tego przykładu?
7. Dlaczego równoległa mutacja `ArrayList` jest niebezpieczna?
8. Czy `parallelStream()` musi być szybszy?

Do zapamiętania:

```text
operacje pośrednie: bez wymaganych efektów ubocznych
peek: diagnostyka, nie logika biznesowa
forEach: nie zakładaj kolejności wykonania
toList na uporządkowanym streamie: zachowuje kolejność spotkania
parallelStream: wymaga poprawnego, bezpiecznego i zmierzonego zastosowania
```

## Podsumowanie Lab 14

Przeszedłeś drogę od podstaw pipeline'u do kolektorów i bezpiecznego
myślenia o równoległości:

```text
Lab14.1 — źródło, pipeline, lazy execution
Lab14.2 — filter, map, flatMap
Lab14.3 — sorted, distinct, skip, limit, wyszukiwanie
Lab14.4 — count, min, max, reduce, Optional
Lab14.5 — toMap, groupingBy, partitioningBy, mapping, teeing
Lab14.6 — efekty uboczne, peek, kolejność, parallelStream
```

Najważniejszy nawyk: buduj pipeline opisujący transformację danych,
zostawiaj efekty uboczne poza operacjami pośrednimi i używaj równoległości
tylko wtedy, gdy semantyka jest poprawna, a pomiar potwierdza korzyść.
