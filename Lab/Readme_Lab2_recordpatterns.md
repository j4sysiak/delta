# Lab 2 — Java 21: Record Patterns

## Cel laba

W tym labie uczymy się **record patterns**, czyli wygodnego rozpakowywania
danych przechowywanych w rekordach.

Po ukończeniu laba będziesz potrafić:

- rozpakować rekord bez ręcznego wywoływania accessorów,
- użyć record pattern w `instanceof`,
- użyć record pattern w `switch`,
- łączyć record patterns z pattern matching,
- rozpakować zagnieżdżone rekordy,
- wyjaśnić zastosowanie record patterns na rozmowie technicznej.

Ten lab jest materiałem szkoleniowym. Nie zmienia kodu aplikacji mini-banku.

## Wymagania

Record patterns są elementem Java 21. Przykłady z tego laba wymagają:

- JDK 21,
- kompilatora ustawionego na Java 21,
- istniejącej konfiguracji Gradle projektu.

## 1. Przykładowy model danych

Załóżmy, że mamy rekord reprezentujący pieniądze:

```java
public record Money(
        BigDecimal amount,
        String currency
) {
}
```

Możemy umieścić go w rekordzie operacji:

```java
public record Payment(
        String accountNumber,
        Money money
) {
}
```

Utworzenie obiektu:

```java
Payment payment = new Payment(
        "PLN-1001",
        new Money(new BigDecimal("150.00"), "PLN")
);
```

## 2. Klasyczny sposób odczytu rekordu

Bez record patterns dane odczytujemy przez accessors:

```java
if (payment instanceof Payment) {
    String accountNumber = payment.accountNumber();
    Money money = payment.money();
    BigDecimal amount = money.amount();
    String currency = money.currency();
}
```

Ten kod działa, ale wymaga:

- osobnego sprawdzenia typu,
- osobnego pobrania każdego pola,
- ręcznego rozpakowania zagnieżdżonego rekordu.

## 3. Record pattern w `instanceof`

Java 21 pozwala rozpakować rekord bezpośrednio w warunku:

```java
if (payment instanceof Payment(String accountNumber, Money money)) {
    System.out.println(accountNumber);
    System.out.println(money);
}
```

W tym zapisie:

```java
Payment(String accountNumber, Money money)
```

Java:

1. sprawdza, czy obiekt jest typu `Payment`,
2. pobiera wartości pól rekordu,
3. przypisuje je do zmiennych `accountNumber` i `money`.

## 4. Zagnieżdżony record pattern

Możemy rozpakować również rekord `Money` w tym samym wzorcu:

```java
if (payment instanceof Payment(
        String accountNumber,
        Money(BigDecimal amount, String currency)
)) {
    System.out.println(accountNumber);
    System.out.println(amount);
    System.out.println(currency);
}
```

Jeden wzorzec rozpakowuje dwa poziomy:

```text
Payment
├── accountNumber
└── Money
    ├── amount
    └── currency
```

Nie trzeba pisać:

```java
payment.accountNumber();
payment.money().amount();
payment.money().currency();
```

## 5. Record pattern w `switch`

Record patterns można łączyć z pattern matching w `switch`:

```java
public String describe(Payment payment) {
    return switch (payment) {
        case Payment(
                String accountNumber,
                Money(BigDecimal amount, String currency)
        ) ->
                "PAYMENT " + amount + " " + currency
                        + " FOR " + accountNumber;
    };
}
```

Warto zauważyć, że wzorzec nie tylko rozpoznaje typ `Payment`, ale również
od razu rozpakowuje jego pola.

## 6. Wzorce z użyciem istniejących zmiennych

Nie zawsze musisz rozpakowywać wszystkie pola.

Możesz użyć wzorca typu:

```java
case Payment(String accountNumber, Money money) ->
        "PAYMENT FOR " + accountNumber;
```

Jeśli interesuje Cię tylko jeden element zagnieżdżonego rekordu, pozostałe
elementy możesz pominąć za pomocą `_` dopiero w nowszych wersjach języka,
które wspierają unnamed patterns. W Java 21 bezpieczniej jest nazwać
potrzebne elementy albo użyć klasycznego accessor:

```java
case Payment(String accountNumber, Money money) ->
        "PAYMENT FOR " + accountNumber;
```

Ważne: `_` jako unnamed pattern nie jest elementem, którego należy używać
w podstawowym kodzie Java 21.

## 7. Warunki w record patterns

Record pattern rozpakowuje dane, a warunek biznesowy można dopisać jako
guard w zwykłym `if`:

```java
if (payment instanceof Payment(
        String accountNumber,
        Money(BigDecimal amount, String currency)
)) {
    if ("PLN".equals(currency) && amount.signum() > 0) {
        return "VALID PLN PAYMENT FOR " + accountNumber;
    }
}
```

Record pattern nie zastępuje walidacji. Jego zadaniem jest wygodne
rozpakowanie struktury danych.

## 8. Przykład związany z mini-bankiem

Możemy opisać operację bankową za pomocą zagnieżdżonych rekordów:

```java
public record AccountReference(String number) {
}
```

```java
public record Money(
        BigDecimal amount,
        String currency
) {
}
```

```java
public record DepositCommand(
        AccountReference account,
        Money money
) {
}
```

Rozpakowanie klasycznym stylem:

```java
DepositCommand command = ...;

String accountNumber = command.account().number();
BigDecimal amount = command.money().amount();
String currency = command.money().currency();
```

Rozpakowanie przez record pattern:

```java
if (command instanceof DepositCommand(
        AccountReference(String accountNumber),
        Money(BigDecimal amount, String currency)
)) {
    System.out.println(accountNumber);
    System.out.println(amount);
    System.out.println(currency);
}
```

## 9. Record patterns a `null`

Record pattern nie dopasuje `null`.

Przykład:

```java
Object value = null;

if (value instanceof Money(BigDecimal amount, String currency)) {
    // Ten blok nie zostanie wykonany.
}
```

W `switch` trzeba świadomie obsłużyć `null`, jeśli aplikacja dopuszcza taką
wartość:

```java
return switch (value) {
    case null -> "NO VALUE";
    case Money(BigDecimal amount, String currency) ->
            amount + " " + currency;
    default -> "UNKNOWN VALUE";
};
```

W kodzie domenowym lepiej wcześniej jasno ustalić, czy `null` jest dozwolony.

## 10. Record patterns nie zmieniają rekordów

Record pattern tylko odczytuje dane.

Nie można przez niego zmienić rekordu:

```java
if (payment instanceof Payment(
        String accountNumber,
        Money(BigDecimal amount, String currency)
)) {
    // accountNumber, amount i currency są lokalnymi zmiennymi.
}
```

Rekord nadal pozostaje obiektem z finalnymi polami.

Jeżeli `Money` zawiera `BigDecimal`, sam rekord jest niemutowalnym kontenerem
referencji. `BigDecimal` jest niemutowalny, dlatego ten przykład jest
bezpieczny pod względem wartości pieniężnej.

## 11. Record patterns a dziedziczenie

Record nie może rozszerzać innej klasy poza `java.lang.Record`, ale może
implementować interfejs.

Przykład:

```java
public record DepositCommand(
        AccountReference account,
        Money money
) implements BankOperation {
}
```

Record pattern rozpakowuje pola konkretnego rekordu, niezależnie od tego,
czy rekord implementuje interfejs.

## 12. Typowe pytania rekrutacyjne

### Czym jest record pattern?

To wzorzec, który pozwala jednocześnie sprawdzić typ rekordu i rozpakować
jego komponenty do lokalnych zmiennych.

### Jaka jest różnica między `instanceof` a record pattern?

Klasyczne `instanceof` tylko sprawdza typ. Record pattern sprawdza typ
i od razu rozpakowuje wartości rekordu.

### Czy record pattern tworzy kopię obiektu?

Nie. Odczytuje komponenty istniejącego rekordu i przypisuje ich wartości do
lokalnych zmiennych.

### Czy record pattern może rozpakować zagnieżdżony rekord?

Tak. Wzorce można zagnieżdżać:

```java
Payment(
        String accountNumber,
        Money(BigDecimal amount, String currency)
)
```

### Co dzieje się dla `null`?

`null` nie pasuje do zwykłego record pattern. W `switch` można obsłużyć go
osobnym przypadkiem `case null`.

### Czy record pattern zastępuje walidację?

Nie. Record pattern rozpakowuje dane. Walidacja poprawności kwoty, waluty
albo numeru konta nadal należy do logiki domenowej.

## 13. Ćwiczenia do samodzielnego wykonania

1. Utwórz rekord:

   ```java
   public record CurrencyAmount(
           BigDecimal amount,
           String currency
   ) {
   }
   ```

2. Utwórz rekord:

   ```java
   public record AccountBalance(
           String accountNumber,
           CurrencyAmount balance
   ) {
   }
   ```

3. Napisz metodę, która używa record pattern i zwraca tekst:

   ```text
   ACCOUNT PLN-1001 HAS BALANCE 250.00 PLN
   ```

4. Dodaj obsługę `null`.

5. Napisz test Spock sprawdzający:

   - poprawne rozpakowanie danych,
   - poprawny wynik dla PLN,
   - poprawny wynik dla USD,
   - zachowanie dla `null`.

## Podsumowanie

W tym labie uczymy się:

```text
record pattern
pattern matching instanceof
pattern matching switch
zagnieżdżone rekordy
obsługa null
Groovy/Spock jako narzędzie do testowania
```

Najważniejszy przykład:

```java
if (command instanceof DepositCommand(
        AccountReference(String accountNumber),
        Money(BigDecimal amount, String currency)
)) {
    // Tutaj mamy już rozpakowane wartości.
}
```

Na rozmowie technicznej warto podkreślić, że record patterns poprawiają
czytelność kodu przy pracy z niemutowalnymi strukturami danych, szczególnie
gdy rekordy są zagnieżdżone i występują w zamkniętym modelu domenowym.
