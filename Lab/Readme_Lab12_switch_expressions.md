# Lab 12 — Switch expressions i `yield`

## Cel laba

Poznasz `switch`, który zwraca wartość, oraz `yield`, który pozwala
zwrócić wynik z bloku jednej gałęzi.

Switch expressions są finalne od Java 14, więc są dostępne również
w Java 17 i Java 21. To ćwiczenie nie wymaga preview, Springa ani bazy.
Pozostaw jednak konfigurację preview w projekcie dla wcześniejszych labów.

Zbudujemy osobny, szkoleniowy kalkulator opłat. Nie zmieniamy rzeczywistych
zasad mini-banku.

## 1. `switch` jako wyrażenie

Wyrażenie daje wynik, który można przypisać do zmiennej lub zwrócić z metody:

```java
var fee = switch (operation) {
    case DEPOSIT -> new BigDecimal("0.00");
    case WITHDRAW -> new BigDecimal("1.00");
    case TRANSFER -> new BigDecimal("2.00");
};
```

Każda gałąź po `->` daje wartość. Nie potrzebujemy `break` i nie ma
automatycznego przechodzenia do następnej gałęzi.

Zwróć uwagę na średnik po zamykającym `}`: kończy przypisanie.

## 2. `yield`: wynik z bloku gałęzi

Jeśli gałąź potrzebuje kilku instrukcji, użyj bloku i `yield`:

```java
case TRANSFER -> {
    var percentageFee = amount.multiply(new BigDecimal("0.005"));
    yield percentageFee.max(new BigDecimal("2.00"));
}
```

Czytaj to tak:

> Oblicz opłatę procentową, porównaj ją z minimalną opłatą i przekaż
> większą z tych wartości jako wynik całego wyrażenia `switch`.

`yield` kończy tę gałąź i daje wynik switcha. Nie kończy całej metody,
tak jak `return`.

## 3. Zasady naszego ćwiczenia

| Operacja | Opłata |
|---|---|
| DEPOSIT | 0.00 PLN |
| WITHDRAW | 1.00 PLN |
| TRANSFER | 0.5% kwoty, minimum 2.00 PLN |

Kwota operacji musi być dodatnia. Wynik zaokrąglamy do dwóch miejsc
po przecinku przez `RoundingMode.HALF_UP`.

To umowne zasady szkoleniowe, nie rzeczywista taryfa banku.

## 4. Enum do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\switchexpressions\BankOperationType.java
```

```java
package com.delta.bank.lab.java21.switchexpressions;

public enum BankOperationType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER
}
```

Używamy własnego enuma tylko dla laba. Nie zmieniaj enuma w domenie banku.

## 5. Kalkulator do wklejenia

Plik:

```text
src\main\java\com\delta\bank\lab\java21\switchexpressions\BankFeeCalculator.java
```

```java
package com.delta.bank.lab.java21.switchexpressions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class BankFeeCalculator {

    public BigDecimal calculate(BankOperationType operation, BigDecimal amount) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        var fee = switch (operation) {
            case DEPOSIT -> new BigDecimal("0.00");
            case WITHDRAW -> new BigDecimal("1.00");
            case TRANSFER -> {
                var percentageFee = amount.multiply(new BigDecimal("0.005"));
                var minimumFee = new BigDecimal("2.00");
                yield percentageFee.max(minimumFee);
            }
        };

        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
```

Prześledź przykład `TRANSFER` dla kwoty `1000.00`:

```text
percentageFee = 1000.00 × 0.005 = 5.00000
minimumFee = 2.00
yield daje większą wartość: 5.00000
fee otrzymuje tę wartość
return zwraca 5.00 po ustawieniu skali
```

Wszystkie obliczenia wykonujemy przez `BigDecimal`, bez `double`.

## 6. Dlaczego nie ma `default`?

Switch expression musi być wyczerpujące: dawać wynik dla wszystkich
obsługiwanych wartości.

Nasz enum ma trzy stałe i każdą wymieniliśmy, więc `default` nie jest
potrzebny. Jeśli dodasz nową stałą enuma i ponownie skompilujesz kod,
kompilator wskaże, że switch wymaga uzupełnienia.

`null` sprawdzamy osobno przez `Objects.requireNonNull`. Wyczerpujący
switch po stałych enuma nie oznacza automatycznej obsługi `null`.

## 7. Spec Spocka do wklejenia

Plik:

```text
src\test\groovy\com\delta\bank\lab\java21\switchexpressions\BankFeeCalculatorSpec.groovy
```

```groovy
package com.delta.bank.lab.java21.switchexpressions

import spock.lang.Specification
import spock.lang.Unroll

class BankFeeCalculatorSpec extends Specification {

    @Unroll
    def "calculates fee #expected for #operation and amount #amount"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        def fee = calculator.calculate(operation, new BigDecimal(amount))

        then:
        fee == new BigDecimal(expected)
        fee.scale() == 2

        where:
        operation                  | amount    | expected
        BankOperationType.DEPOSIT  | "1000.00" | "0.00"
        BankOperationType.WITHDRAW | "1000.00" | "1.00"
        BankOperationType.TRANSFER | "100.00"  | "2.00"
        BankOperationType.TRANSFER | "400.00"  | "2.00"
        BankOperationType.TRANSFER | "1000.00" | "5.00"
        BankOperationType.TRANSFER | "1001.00" | "5.01"
    }

    @Unroll
    def "rejects non-positive amount #amount"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        calculator.calculate(BankOperationType.TRANSFER, new BigDecimal(amount))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "amount must be positive"

        where:
        amount << ["0.00", "-10.00"]
    }

    @Unroll
    def "rejects null #argument"() {
        given:
        def calculator = new BankFeeCalculator()

        when:
        calculator.calculate(operation, amount)

        then:
        def exception = thrown(NullPointerException)
        exception.message == message

        where:
        argument    | operation                 | amount                  | message
        "operation" | null                      | new BigDecimal("10.00") | "operation must not be null"
        "amount"    | BankOperationType.DEPOSIT | null                    | "amount must not be null"
    }
}
```

Przypadek `1001.00` daje `5.00500` przed zaokrągleniem, czyli `5.01`
po zastosowaniu `HALF_UP`.

## 8. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.switchexpressions.BankFeeCalculatorSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.switchexpressions.BankFeeCalculatorSpec"
```

Ten spec nie potrzebuje bazy ani kontekstu Springa.

## 9. Eksperyment: kompilator pilnuje kompletności

Dodaj na chwilę czwartą stałą enuma:

```java
public enum BankOperationType {
    DEPOSIT,
    WITHDRAW,
    TRANSFER,
    PAYMENT
}
```

Uruchom test. Kompilacja powinna się nie udać, ponieważ kalkulator nie
określa wyniku dla `PAYMENT`.

Usuń `PAYMENT`, aby wrócić do działającego przykładu.

Nie dodawaj `default -> new BigDecimal("0.00")` tylko po to, aby ukryć ten
błąd. Mogłoby to przypadkowo nadać nowej operacji darmową opłatę.

## 10. Pytania rekrutacyjne

### Czym switch expression różni się od switch statement?

Wyrażenie zwraca wartość: można jej użyć w przypisaniu albo jako wynik
metody. Instrukcja steruje wykonaniem kodu, ale sama nie daje wartości.

### Do czego służy `yield`?

Przekazuje wynik z wieloinstrukcyjnego bloku gałęzi do wyrażenia switch.

### Czy `yield` jest tym samym co `return`?

Nie. `yield` daje wynik wyrażenia switch, a `return` kończy metodę.

### Czy po gałęzi `->` potrzebny jest `break`?

Nie. Nie następuje automatyczne przejście do kolejnej gałęzi.

### Czy zawsze trzeba pisać `default`?

Nie. Dla enuma można jawnie obsłużyć wszystkie stałe. Kompilator pilnuje
wyczerpującego charakteru wyrażenia.

### Czy switch expressions wymagają preview w Java 21?

Nie. Są finalne od Java 14.

## Do zapamiętania

```text
switch expression = switch daje wynik.
-> = gałąź bez automatycznego przechodzenia dalej.
yield = wynik z bloku gałęzi.
return = wynik całej metody.
```
