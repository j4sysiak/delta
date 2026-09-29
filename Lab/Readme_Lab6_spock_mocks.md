# Lab 6 — Groovy/Spock: Mocki i interakcje

## Cel laba

W tym labie uczymy się testować logikę serwisu w izolacji, bez uruchamiania
PostgreSQL. Rzeczywiste repozytorium zastąpimy mockiem Spocka.

Schemat:

```text
BankAccountService
        ↓
mock BankAccountRepository
```

Test jednostkowy sprawdzi zachowanie serwisu oraz jego komunikację
z zależnością.

## Czego się nauczysz

- `Mock(...)`,
- `Stub(...)`,
- `Spy(...)`,
- interakcji Spocka,
- liczby wywołań:
  - `1 *`,
  - `0 *`,
  - `2 *`,
- dopasowania argumentów przez `_`,
- dopasowania argumentów przez closure,
- zwracania wartości przez `>>`,
- rzucania wyjątków przez `>> { throw ... }`,
- sprawdzania kolejności wywołań,
- różnicy między testem jednostkowym a integracyjnym.

## 1. Mock

Mock służy przede wszystkim do sprawdzania interakcji:

```groovy
def repository = Mock(BankAccountRepository)
```

Dokładnie jedno wywołanie:

```groovy
1 * repository.findByAccountNumber("PLN-1001")
```

Wywołanie z przygotowaną odpowiedzią:

```groovy
1 * repository.findByAccountNumber("PLN-1001") >>
        Optional.of(account)
```

## 2. Stub

Stub służy głównie do dostarczania odpowiedzi:

```groovy
def repository = Stub(BankAccountRepository) {
    findByAccountNumber(_) >> Optional.of(account)
}
```

Stub nie jest przeznaczony przede wszystkim do weryfikacji liczby wywołań.

## 3. Spy

Spy opakowuje prawdziwy obiekt:

```groovy
def spy = Spy(realObject)
```

Można wtedy użyć rzeczywistej implementacji i nadpisać wybrane metody.

## 4. Liczba wywołań

```groovy
1 * repository.save(_)
```

Metoda musi zostać wywołana dokładnie raz.

```groovy
0 * repository.delete(_)
```

Metoda nie może zostać wywołana.

```groovy
2 * repository.findByAccountNumber(_)
```

Metoda musi zostać wywołana dokładnie dwa razy.

```groovy
0 * _
```

Żadne inne, nieopisane interakcje nie są dozwolone.

## 5. Dopasowanie argumentów

Dowolny argument:

```groovy
1 * repository.save(_)
```

Konkretny argument:

```groovy
1 * repository.findByAccountNumber("PLN-1001")
```

Warunek na argumencie:

```groovy
1 * repository.save({
    it.accountNumber == "PLN-1001"
})
```

## 6. Zwracanie wartości

```groovy
1 * repository.findByAccountNumber("PLN-1001") >>
        Optional.of(account)
```

Wartość zależna od argumentu:

```groovy
repository.findByAccountNumber(_) >> { String number ->
    Optional.of(createAccount(number))
}
```

## 7. Rzucanie wyjątku

```groovy
1 * repository.save(_) >>
        { throw new IllegalStateException("Database unavailable") }
```

Test może sprawdzić, czy serwis propaguje albo mapuje wyjątek.

## 8. Kolejność interakcji

```groovy
then:
1 * repository.findByAccountNumber("PLN-1001")
1 * repository.save(_)
```

Opisuje kolejność:

1. odczyt konta,
2. zapis konta.

## 9. Mock a test integracyjny

### Test jednostkowy z mockiem

```text
serwis + mock repozytorium
```

Jest szybki, izolowany i nie wymaga PostgreSQL.

### Test integracyjny

```text
serwis + prawdziwe repozytorium + PostgreSQL
```

Sprawdza JPA, SQL, migracje i rzeczywistą komunikację z bazą.

Oba rodzaje testów mają inne zadanie i powinny istnieć obok siebie.

## 10. Pytania rekrutacyjne

### Czym różni się mock od stubu?

Mock służy głównie do weryfikowania interakcji, a stub dostarcza przygotowane
odpowiedzi.

### Co oznacza `1 * repository.save(_)`?

`save` musi zostać wywołane dokładnie raz z dowolnym argumentem.

### Czy mock testuje bazę?

Nie. Mock zastępuje repozytorium. Test sprawdza logikę serwisu w izolacji.

### Kiedy potrzebny jest test integracyjny?

Gdy trzeba sprawdzić rzeczywistą współpracę z JPA, PostgreSQL, migracjami
i zapytaniami.

### Czy Spock sprawdza kolejność interakcji?

Tak. Interakcje opisane w kolejności weryfikowanej przez specyfikację
pozwalają kontrolować kolejność komunikacji z zależnością.

## 11. Uruchomienie

Po utworzeniu speca uruchom:

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.SpockMocksSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

Najważniejsze elementy laba:

```text
Mock
Stub
Spy
1 *
0 *
_
>>
argument matchers
kolejność interakcji
test jednostkowy bez bazy danych
```
