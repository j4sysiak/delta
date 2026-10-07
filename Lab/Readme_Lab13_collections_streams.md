# Lab 13 — Kolekcje i strumienie

## Cel laba

Poznasz cztery API dostępne w Java 21, które pojawiły się wcześniej:

| API | Od wersji |
|---|---|
| `List.of(...)` | Java 9 |
| `Map.of(...)` | Java 9 |
| `Collectors.teeing(...)` | Java 12 |
| `Stream.toList()` | Java 16 |

To kontynuacja ćwiczeń z funkcji dostępnych również w Java 17.
Nie potrzebujemy preview, wątków, Springa ani bazy danych.
Pozostaw konfigurację preview w projekcie dla wcześniejszych labów.

W przykładzie utworzymy listę wpłat i obliczymy ich liczbę oraz sumę.
Nie zmieniamy produkcyjnej historii transakcji mini-banku.

## 1. `List.of(...)`: krótki zapis listy

```java
var deposits = List.of(
        new BigDecimal("100.00"),
        new BigDecimal("200.00"),
        new BigDecimal("50.00")
);
```

Lista:

- zachowuje podaną kolejność,
- nie pozwala dodawać, usuwać ani zastępować elementów,
- nie dopuszcza `null`,
- może zawierać powtarzające się wartości.

Ta operacja rzuci `UnsupportedOperationException`:

```java
deposits.add(new BigDecimal("10.00"));
```

Jeśli potrzebujesz modyfikowalnej listy, utwórz ją jawnie:

```java
var editableDeposits = new ArrayList<>(deposits);
editableDeposits.add(new BigDecimal("10.00"));
```

Brak możliwości zmiany kolekcji nie oznacza, że jej elementy są
niemutowalne. W tym labie używamy `BigDecimal`, który sam jest niemutowalny.

## 2. `Map.of(...)`: mała mapa

```java
var labels = Map.of(
        "DEPOSIT", "Wplata",
        "WITHDRAW", "Wyplata"
);
```

Mapa:

- nie pozwala zmieniać wpisów,
- nie dopuszcza kluczy ani wartości `null`,
- odrzuca powtarzające się klucze,
- nie gwarantuje kolejności iteracji.

`Map.of(...)` ma przeciążenia dla maksymalnie dziesięciu par.
Do większej liczby wpisów służy `Map.ofEntries(...)`.

Nie używaj kolejności drukowania tej mapy jako kontraktu testu.

## 3. `Stream.toList()`: zbierz wynik przetwarzania

```java
var selected = deposits.stream()
        .filter(amount -> amount.compareTo(new BigDecimal("100.00")) >= 0)
        .toList();
```

Wynik to lista wpłat o wartości co najmniej `100.00`.

Ważne:

- `toList()` jest operacją terminalną,
- zachowuje kolejność strumienia, jeśli strumień ma określoną kolejność,
- zwraca listę niemodyfikowalną,
- nie gwarantuje konkretnej klasy implementacji,
- w przeciwieństwie do `List.of(...)` dopuszcza elementy `null`.

`toList()` nie wykonuje głębokiej kopii elementów. Nie jest też widokiem
listy źródłowej: późniejsze dodanie elementu do źródła nie dodaje go do
już zebranego wyniku.

Nie utożsamiaj go z `collect(Collectors.toList())`: ten kolektor nie daje
gwarancji dotyczących modyfikowalności wyniku. Jeżeli wymagana jest
modyfikowalna lista, użyj jawnie:

```java
.collect(Collectors.toCollection(ArrayList::new))
```

## 4. `Collectors.teeing(...)`: dwa obliczenia z jednego strumienia

Chcemy jednocześnie uzyskać:

```text
liczbę wpłat
sumę wpłat
```

`teeing` przyjmuje:

1. Kolektor pierwszego wyniku.
2. Kolektor drugiego wyniku.
3. Funkcję łączącą oba wyniki.

Schemat:

```text
strumień wpłat
├── counting() → liczba
└── reducing(...) → suma
        połącz wyniki w DepositSummary
```

To nie tworzy automatycznie wątków. W naszym przykładzie jest to zwykłe
przetwarzanie sekwencyjne.

Nie próbuj wykonywać dwóch operacji terminalnych na tej samej instancji
strumienia. Strumień jest jednorazowy.

## 5. Rekord do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\streams\DepositSummary.java
```

```java
package com.delta.bank.lab.java21.streams;

import java.math.BigDecimal;

public record DepositSummary(long count, BigDecimal total) {
}
```

`counting()` zwraca `Long`, dlatego w rekordzie używamy `long`.

## 6. Klasa do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\streams\DepositAnalyzer.java
```

```java
package com.delta.bank.lab.java21.streams;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DepositAnalyzer {

    public List<BigDecimal> atLeast(
            List<BigDecimal> deposits,
            BigDecimal minimum
    ) {
        validateDeposits(deposits);
        Objects.requireNonNull(minimum, "minimum must not be null");

        return deposits.stream()
                .filter(amount -> amount.compareTo(minimum) >= 0)
                .toList();
    }

    public DepositSummary summarize(List<BigDecimal> deposits) {
        validateDeposits(deposits);

        return deposits.stream()
                .collect(Collectors.teeing(
                        Collectors.counting(),
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add),
                        DepositSummary::new
                ));
    }

    private void validateDeposits(List<BigDecimal> deposits) {
        Objects.requireNonNull(deposits, "deposits must not be null");

        for (var amount : deposits) {
            Objects.requireNonNull(amount, "deposit must not be null");

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("deposit must be positive");
            }
        }
    }
}
```

Czytaj `summarize(...)` tak:

```text
counting()                              → policz elementy
reducing(BigDecimal.ZERO, BigDecimal::add) → dodaj kwoty, zaczynając od zera
DepositSummary::new                     → utwórz rekord z obu wyników
```

`DepositSummary::new` jest referencją do konstruktora. Odpowiada lambdzie:

```java
(count, total) -> new DepositSummary(count, total)
```

Pusta lista jest poprawna: jej podsumowanie to liczba `0` i suma `0`.
Każdy element niepustej listy musi być dodatnią kwotą, różną od `null`.

Walidacja przechodzi przez listę przed obliczeniem. `teeing` zbiera dwa
wyniki w jednym przebiegu strumienia, ale nie oznacza to, że cała metoda,
łącznie z walidacją, odwiedza elementy tylko raz.

## 7. Spec Spocka do wklejenia

Plik:

```text
src\test\groovy\com\delta\bank\lab\java21\streams\DepositAnalyzerSpec.groovy
```

```groovy
package com.delta.bank.lab.java21.streams

import spock.lang.Specification
import spock.lang.Unroll

import java.util.stream.Stream

class DepositAnalyzerSpec extends Specification {

    def "List.of preserves order and rejects modification"() {
        given:
        def deposits = List.of(
                new BigDecimal("100.00"),
                new BigDecimal("50.00")
        )

        expect:
        deposits == [new BigDecimal("100.00"), new BigDecimal("50.00")]

        when:
        deposits.add(new BigDecimal("10.00"))

        then:
        thrown(UnsupportedOperationException)
    }

    def "List.of rejects null"() {
        when:
        List.of("DEPOSIT", null)

        then:
        thrown(NullPointerException)
    }

    def "Map.of provides labels and rejects modification"() {
        given:
        def labels = Map.of(
                "DEPOSIT", "Wplata",
                "WITHDRAW", "Wyplata"
        )

        expect:
        labels.get("DEPOSIT") == "Wplata"
        labels.get("WITHDRAW") == "Wyplata"
        labels.size() == 2

        when:
        labels.put("TRANSFER", "Przelew")

        then:
        thrown(UnsupportedOperationException)
    }

    def "Map.of rejects duplicate keys"() {
        when:
        Map.of("DEPOSIT", "Wplata", "DEPOSIT", "Inna etykieta")

        then:
        thrown(IllegalArgumentException)
    }

    def "filters deposits preserving order and returns an unmodifiable list"() {
        given:
        def analyzer = new DepositAnalyzer()
        def deposits = List.of(
                new BigDecimal("200.00"),
                new BigDecimal("50.00"),
                new BigDecimal("100.00")
        )

        when:
        def selected = analyzer.atLeast(deposits, new BigDecimal("100.00"))

        then:
        selected == [new BigDecimal("200.00"), new BigDecimal("100.00")]

        when:
        selected.add(new BigDecimal("300.00"))

        then:
        thrown(UnsupportedOperationException)
    }

    def "Stream.toList result does not follow structural changes to the source"() {
        given:
        def source = new ArrayList<String>(["DEPOSIT", "WITHDRAW"])

        when:
        def result = source.stream().toList()
        source.add("TRANSFER")

        then:
        result == ["DEPOSIT", "WITHDRAW"]
        source == ["DEPOSIT", "WITHDRAW", "TRANSFER"]
    }

    def "Stream.toList allows null elements"() {
        when:
        def result = Stream.of("DEPOSIT", null).toList()

        then:
        result.size() == 2
        result.get(0) == "DEPOSIT"
        result.get(1) == null
    }

    def "summarizes count and total using teeing"() {
        given:
        def analyzer = new DepositAnalyzer()
        def deposits = List.of(
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                new BigDecimal("50.00")
        )

        when:
        def summary = analyzer.summarize(deposits)

        then:
        summary.count() == 3L
        summary.total() == new BigDecimal("350.00")
    }

    def "summarizes an empty list"() {
        when:
        def summary = new DepositAnalyzer().summarize(List.of())

        then:
        summary.count() == 0L
        summary.total() == BigDecimal.ZERO
    }

    @Unroll
    def "rejects non-positive deposit #amount"() {
        when:
        new DepositAnalyzer().summarize(List.of(new BigDecimal(amount)))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "deposit must be positive"

        where:
        amount << ["0.00", "-10.00"]
    }

    def "rejects null deposits list"() {
        when:
        new DepositAnalyzer().summarize(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "deposits must not be null"
    }

    def "rejects null deposit inside a list"() {
        given:
        def deposits = new ArrayList<BigDecimal>()
        deposits.add(null)

        when:
        new DepositAnalyzer().summarize(deposits)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "deposit must not be null"
    }

    def "rejects null minimum"() {
        when:
        new DepositAnalyzer().atLeast(List.of(), null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "minimum must not be null"
    }
}
```

Do testu wartości `null` w analizatorze tworzymy `ArrayList`. Gdybyśmy
użyli `List.of(null)`, wyjątek pochodziłby z tworzenia listy, a nie z
walidacji analizatora.

## 8. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.streams.DepositAnalyzerSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.streams.DepositAnalyzerSpec"
```

Ten spec nie wymaga bazy danych ani kontekstu Springa.

## 9. Eksperyment: modyfikowalny wynik

Na chwilę zmień zakończenie strumienia w `atLeast(...)`:

```java
.collect(Collectors.toCollection(java.util.ArrayList::new));
```

Uruchom spec. Test oczekujący `UnsupportedOperationException` po dodaniu
elementu do wyniku nie przejdzie, ponieważ wynik jest teraz modyfikowalny.

Przywróć `.toList()`.

To nie błąd kolektora: zmieniłeś kontrakt zwracanej kolekcji.

## 10. Pytania rekrutacyjne

### Czy `List.of(...)` daje modyfikowalną listę?

Nie. Nie można dodawać, usuwać ani zastępować elementów.
Elementy same w sobie mogą jednak być mutowalne.

### Czy `Map.of(...)` gwarantuje kolejność?

Nie. Nie należy polegać na kolejności iteracji wpisów.

### Czy `Stream.toList()` daje ten sam kontrakt co `Collectors.toList()`?

Nie. `Stream.toList()` gwarantuje niemodyfikowalny wynik.
`Collectors.toList()` nie gwarantuje modyfikowalności ani konkretnego typu.

### Jak jawnie uzyskać modyfikowalny wynik strumienia?

Użyć `Collectors.toCollection(ArrayList::new)`.

### Czy `List.of(...)` i `Stream.toList()` dopuszczają `null`?

`List.of(...)` nie dopuszcza `null`; `Stream.toList()` dopuszcza.
Kontrakt metody biznesowej może dodatkowo zabraniać takich elementów,
tak jak w naszym analizatorze.

### Do czego służy `teeing`?

Przekazuje elementy do dwóch kolektorów i łączy ich końcowe wyniki
za pomocą podanej funkcji.

### Czy `teeing` oznacza przetwarzanie wielowątkowe?

Nie. Współbieżność nie wynika z samego użycia tego kolektora.

## Do zapamiętania

```text
List.of / Map.of = małe kolekcje bez możliwości modyfikacji.
Stream.toList = niemodyfikowalna lista wyników strumienia.
teeing = dwa obliczenia i połączenie ich wyników.
```
