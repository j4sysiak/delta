# Lab 5 — Groovy/Spock: Data Tables

## Cel laba

W tym labie uczymy się tabel danych w Spocku. Data tables pozwalają
przetestować wiele przypadków jednym scenariuszem zamiast tworzyć wiele
niemal identycznych metod testowych.

Przykład:

```groovy
where:
currency || valid
"PLN"    || true
"EUR"    || true
"pln"    || false
```

Spock uruchomi test osobno dla każdego wiersza tabeli.

## 1. Problem powtarzalnych testów

Bez tabel danych można napisać:

```groovy
def "accepts PLN"() {
    expect:
    isValidCurrency("PLN")
}

def "accepts EUR"() {
    expect:
    isValidCurrency("EUR")
}

def "rejects lowercase currency"() {
    expect:
    !isValidCurrency("pln")
}
```

Testy są czytelne, ale powtarzają strukturę.

## 2. Jedna specyfikacja i wiele danych

```groovy
def "currency should have expected validity"() {
    expect:
    isValidCurrency(currency) == valid

    where:
    currency || valid
    "PLN"    || true
    "EUR"    || true
    "USD"    || true
    "pln"    || false
    "EURO"   || false
    ""       || false
}
```

Każdy wiersz sekcji `where:` jest osobnym przypadkiem testowym.

## 3. Kolumny tabeli

Nagłówek:

```groovy
currency || valid
```

definiuje dwie kolumny:

- `currency` — dane wejściowe,
- `valid` — oczekiwany wynik.

Operator `||` oddziela kolumny tabeli.

Można użyć większej liczby kolumn:

```groovy
amount  | currency | valid
100.00  | "PLN"    | true
0.00    | "PLN"    | false
-10.00  | "PLN"    | false
```

Operator `|` również rozdziela kolumny. W praktyce `||` często poprawia
czytelność tabeli z wyraźnym oddzieleniem danych od oczekiwanego wyniku.

## 4. `@Unroll`

Adnotacja `@Unroll` pozwala umieścić wartości parametrów w nazwie przypadku:

```groovy
import spock.lang.Unroll

@Unroll
def "currency #currency should be valid: #valid"() {
    expect:
    isValidCurrency(currency) == valid

    where:
    currency || valid
    "PLN"    || true
    "EUR"    || true
    "pln"    || false
}
```

Raport testów może wtedy pokazać osobne nazwy:

```text
currency PLN should be valid: true
currency EUR should be valid: true
currency pln should be valid: false
```

Bez `@Unroll` Spock nadal wykona wszystkie wiersze, ale raport może być mniej
szczegółowy.

## 5. Data tables a mini-bank

Data tables dobrze pasują do reguł bankowych:

### Waluta

```groovy
where:
currency || valid
"PLN"    || true
"EUR"    || true
"USD"    || true
"pln"    || false
"EURO"   || false
""       || false
```

### Kwota

```groovy
where:
amount               || valid
new BigDecimal("1")  || true
new BigDecimal("0")  || false
new BigDecimal("-1") || false
```

### Wypłata

```groovy
where:
balance              | withdrawal          | allowed
new BigDecimal("100") | new BigDecimal("50") | true
new BigDecimal("100") | new BigDecimal("100") | true
new BigDecimal("100") | new BigDecimal("101") | false
```

## 6. Bloki Spocka w teście tabelarycznym

Test tabelaryczny może mieć pełną strukturę:

```groovy
@Unroll
def "withdrawal from #balance by #withdrawal should be allowed: #allowed"() {
    given:
    def account = new TestAccount(balance)

    when:
    account.withdraw(withdrawal)

    then:
    noExceptionThrown() == allowed

    where:
    balance                | withdrawal             | allowed
    new BigDecimal("100")  | new BigDecimal("50")   | true
    new BigDecimal("100")  | new BigDecimal("100")  | true
    new BigDecimal("100")  | new BigDecimal("101")  | false
}
```

Warto pamiętać, że przy sprawdzaniu wyjątków często lepiej rozdzielić
przypadki sukcesu i błędu albo użyć pomocniczej metody testowej.

## 7. Dobre praktyki

- Każdy wiersz powinien reprezentować jeden zrozumiały przypadek biznesowy.
- Nazwy kolumn powinny wyjaśniać ich znaczenie.
- Tabela nie powinna zawierać przypadków całkowicie niezwiązanych ze sobą.
- Długą tabelę warto podzielić na kilka testów.
- Warto używać `@Unroll`, gdy wartości parametrów pomagają znaleźć błąd.
- Dla pieniędzy używaj `BigDecimal`, nie `double`.
- Nie testuj tylko szczęśliwej ścieżki.
- Dodawaj wartości graniczne: zero, kwotę równą saldu i kwotę przekraczającą saldo.

## 8. Najczęstsze błędy

### Brak kolumny w nagłówku

Każda wartość w wierszu musi odpowiadać kolumnie:

```groovy
where:
amount || valid
100    || true
```

### Używanie `double` dla pieniędzy

Nie zalecamy:

```groovy
100.10d
```

Lepsze jest:

```groovy
new BigDecimal("100.10")
```

### Zbyt wiele niepowiązanych przypadków

Jedna tabela powinna odpowiadać jednemu zachowaniu. Osobne tabele mogą
testować walutę, kwotę i wypłatę.

## 9. Pytania rekrutacyjne

### Czym jest Data Table w Spocku?

To tabela parametrów w sekcji `where:`, która uruchamia tę samą specyfikację
dla wielu zestawów danych.

### Co robi `@Unroll`?

Rozwija kolejne wiersze tabeli do osobnych przypadków widocznych w raporcie.

### Czy każdy wiersz jest osobnym testem?

Logicznie tak: Spock wykonuje specyfikację osobno dla każdego wiersza.

### Gdzie definiuje się dane parametryczne?

W bloku `where:`.

### Kiedy nie używać jednej dużej tabeli?

Gdy przypadki testują różne zachowania albo tabela staje się trudna do
czytania. Wtedy lepiej podzielić ją na kilka specyfikacji.

## 10. Ćwiczenia

1. Dodaj tabelę poprawnych i niepoprawnych walut.
2. Dodaj tabelę kwot dodatnich, zerowych i ujemnych.
3. Dodaj przypadek wypłaty dokładnie całego salda.
4. Dodaj przypadek wypłaty większej niż saldo.
5. Dodaj `@Unroll` i sprawdź nazwy przypadków w raporcie.
6. Dodaj kolumnę z opisem przypadku:

   ```groovy
   amount | valid | description
   100    | true  | "positive amount"
   0      | false | "zero amount"
   ```

## Uruchomienie

Po utworzeniu speca uruchom:

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.SpockDataTablesSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.SpockDataTablesSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

Najważniejszy wzorzec Lab 5:

```groovy
@Unroll
def "amount #amount should be valid: #valid"() {
    expect:
    amount.signum() > 0 == valid

    where:
    amount               || valid
    new BigDecimal("1")  || true
    new BigDecimal("0")  || false
    new BigDecimal("-1") || false
}
```

Data tables pozwalają pisać krótsze testy, zwiększać pokrycie przypadków
brzegowych i prezentować reguły biznesowe w formie czytelnej tabeli.
