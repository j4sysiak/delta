# Lab 1 — Java 21: `sealed interface`, `record` i pattern matching

## Cel laba

W tym labie uczymy się trzech ważnych elementów Java 21:

- `sealed interface`
- `record`
- pattern matching:
  - w `switch`
  - w `instanceof`

Przykład wykorzystuje model operacji bankowych, ale działa jako niezależny
komponent szkoleniowy i nie zmienia istniejącej logiki mini-banku.

## Pliki laba

### Kod produkcyjny

```text
src/main/java/com/delta/bank/lab/java21/BankOperation.java
src/main/java/com/delta/bank/lab/java21/DepositOperation.java
src/main/java/com/delta/bank/lab/java21/WithdrawOperation.java
src/main/java/com/delta/bank/lab/java21/TransferOperation.java
src/main/java/com/delta/bank/lab/java21/BankOperationProcessor.java
```

### Test Spock

```text
src/test/groovy/com/delta/bank/BankOperationSpec.groovy
```

## 1. `sealed interface`

Plik:

```text
src/main/java/com/delta/bank/lab/java21/BankOperation.java
```

```java
public sealed interface BankOperation
        permits DepositOperation, WithdrawOperation, TransferOperation {
}
```

`sealed interface` ogranicza listę klas, które mogą implementować interfejs.
W tym przykładzie dozwolone są dokładnie trzy implementacje:

```text
BankOperation
├── DepositOperation
├── WithdrawOperation
└── TransferOperation
```

### Zwykły interfejs a `sealed interface`

Zwykły interfejs:

```java
public interface BankOperation {
}
```

może być implementowany przez dowolną klasę.

Interfejs sealed:

```java
public sealed interface BankOperation
        permits DepositOperation, WithdrawOperation, TransferOperation {
}
```

definiuje zamkniętą hierarchię typów. Dzięki temu kompilator zna pełny zestaw
możliwych wariantów.

## 2. `record`

Przykład:

```java
public record DepositOperation(
        String accountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}
```

`record` jest przeznaczony przede wszystkim do obiektów, których głównym
zadaniem jest przechowywanie danych.

Java automatycznie generuje między innymi:

- konstruktor,
- metody dostępowe,
- `equals`,
- `hashCode`,
- `toString`.

Utworzenie rekordu:

```java
DepositOperation operation = new DepositOperation(
        "PLN-1001",
        new BigDecimal("150.00"),
        "PLN"
);
```

Dostęp do wartości:

```java
operation.accountNumber();
operation.amount();
operation.currency();
```

W rekordzie nie używamy klasycznych getterów typu `getAmount()`.

## 3. Operacje bankowe

### `DepositOperation`

```java
public record DepositOperation(
        String accountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}
```

Reprezentuje wpłatę na konto.

### `WithdrawOperation`

```java
public record WithdrawOperation(
        String accountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}
```

Reprezentuje wypłatę z konta.

### `TransferOperation`

```java
public record TransferOperation(
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}
```

Reprezentuje przelew pomiędzy dwoma kontami.

## 4. Pattern matching w `switch`

Procesor operacji może rozpoznawać wariant operacji bez ręcznego castowania:

```java
public String process(BankOperation operation) {
    return switch (operation) {
        case DepositOperation deposit ->
                "DEPOSIT " + deposit.amount()
                        + " " + deposit.currency()
                        + " TO " + deposit.accountNumber();

        case WithdrawOperation withdraw ->
                "WITHDRAW " + withdraw.amount()
                        + " " + withdraw.currency()
                        + " FROM " + withdraw.accountNumber();

        case TransferOperation transfer ->
                "TRANSFER " + transfer.amount()
                        + " " + transfer.currency()
                        + " FROM " + transfer.fromAccountNumber()
                        + " TO " + transfer.toAccountNumber();
    };
}
```

Instrukcja:

```java
case DepositOperation deposit ->
```

jednocześnie:

1. sprawdza, czy obiekt jest typu `DepositOperation`,
2. przypisuje go do zmiennej `deposit`,
3. pozwala używać tej zmiennej jako właściwego typu.

## 5. Kompletność `switch`

Ponieważ `BankOperation` jest sealed, kompilator zna wszystkie implementacje:

- `DepositOperation`,
- `WithdrawOperation`,
- `TransferOperation`.

Dlatego `switch` może obsłużyć wszystkie warianty bez `default`.

Jeżeli do hierarchii dodamy nową operację, kompilator może wskazać miejsca,
w których trzeba dopisać jej obsługę. Jest to szczególnie przydatne
w zamkniętych modelach domenowych.

## 6. Pattern matching w `instanceof`

Przykład:

```java
if (operation instanceof DepositOperation deposit) {
    return "Deposit for account " + deposit.accountNumber();
}
```

Ten zapis jednocześnie:

- sprawdza typ obiektu,
- tworzy zmienną `deposit`,
- pozwala korzystać z niej jako z `DepositOperation`.

Starszy, bardziej rozwlekły zapis wymagałby dwóch kroków:

```java
if (operation instanceof DepositOperation) {
    DepositOperation deposit = (DepositOperation) operation;
}
```

Pattern matching łączy sprawdzenie i uzyskanie właściwego typu w jednym
wyrażeniu.

## 7. Testy Spock

Testy są napisane w Groovy z użyciem Spocka.

```groovy
def "processes deposit with pattern matching switch"() {
    given:
    def operation = new DepositOperation(
            "PLN-1001",
            new BigDecimal("150.00"),
            "PLN"
    )

    expect:
    processor.process(operation) == "DEPOSIT 150.00 PLN TO PLN-1001"
}
```

Typowa struktura testu Spock:

```text
given:
    przygotowanie danych

when:
    wykonanie operacji

then:
    sprawdzenie wyniku
```

W prostych przypadkach można użyć bezpośrednio bloku `expect:`.

### Co sprawdzają testy

Testy obejmują:

- obsługę wpłaty,
- obsługę wypłaty,
- obsługę przelewu,
- pattern matching przez `instanceof`,
- odrzucenie wartości `null`,
- porównywanie rekordów po wartościach.

### Test wartości `null`

```groovy
def "rejects null operation"() {
    when:
    processor.process(null)

    then:
    def exception = thrown(NullPointerException)
    exception.message == "operation must not be null"
}
```

### Porównywanie rekordów

```groovy
def "records compare by values"() {
    given:
    def first = new DepositOperation(
            "PLN-1001",
            new BigDecimal("100.00"),
            "PLN"
    )

    def second = new DepositOperation(
            "PLN-1001",
            new BigDecimal("100.00"),
            "PLN"
    )

    expect:
    first == second
}
```

Dwa rekordy mają takie same wartości pól, więc są równe.

## 8. Uruchomienie testu

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.BankOperationSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.BankOperationSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## 9. Pytania rekrutacyjne

### Co to jest `sealed interface`?

To interfejs, który ogranicza listę dozwolonych implementacji. Używamy go,
gdy model ma zamknięty zestaw wariantów.

### Co to jest `record`?

To specjalny typ klasy przeznaczony do niemutowalnych obiektów danych.
Java automatycznie generuje konstruktor, metody dostępowe, `equals`,
`hashCode` i `toString`.

### Co daje pattern matching?

Pozwala połączyć sprawdzenie typu z uzyskaniem zmiennej właściwego typu:

```java
if (operation instanceof DepositOperation deposit) {
    return deposit.amount();
}
```

### Dlaczego `switch` nie potrzebuje `default`?

Ponieważ `BankOperation` jest sealed, a kompilator zna wszystkie jego
implementacje.

### Czy `record` jest zawsze w pełni niemutowalny?

Pola rekordu są finalne, ale obiekt przechowywany w polu może być mutowalny.
Przykładowo rekord zawierający `List` nie gwarantuje niemutowalności samej
listy.

## Podsumowanie

W tym labie wykorzystaliśmy:

```text
sealed interface
record
pattern matching instanceof
pattern matching switch
Groovy
Spock
```

Model operacji:

```text
BankOperation
├── DepositOperation
├── WithdrawOperation
└── TransferOperation
```

Procesor rozpoznaje typ operacji bez ręcznego castowania, a testy Spock
opisują zachowanie w czytelnej formie biznesowej.
