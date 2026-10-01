# Lab 8 — Java 21: Sequenced Collections

## Cel laba

W tym labie poznajemy nowe w Java 21 interfejsy sekwencjonowanych kolekcji:

- `SequencedCollection`
- `SequencedSet`
- `SequencedMap`

Użyjemy uporządkowanej historii transakcji, aby przećwiczyć dostęp do
pierwszego i ostatniego elementu oraz odwrócony widok kolekcji.

To będzie odizolowane ćwiczenie szkoleniowe. Nie zmieniamy istniejącego
mini-banku ani jego historii transakcji.

## Dlaczego dodano Sequenced Collections?

Przed Java 21 różne uporządkowane kolekcje oferowały różne sposoby dostępu
do swoich końców. Java 21 wprowadziła wspólne interfejsy, które wyrażają
ważny kontrakt:

> Kolekcja ma zdefiniowany encounter order — kolejność, w której elementy
> są napotykane podczas iteracji.

Dzięki temu można w spójny sposób pobierać pierwszy i ostatni element albo
uzyskać widok z odwróconą kolejnością.

## 1. `SequencedCollection`

`SequencedCollection` rozszerza `Collection` i udostępnia metody operujące
na początku i końcu sekwencji:

```java
collection.getFirst();
collection.getLast();
collection.reversed();
```

Interfejs deklaruje również operacje dodawania i usuwania elementów na
końcach, jeśli dana implementacja je obsługuje:

```java
collection.addFirst(element);
collection.addLast(element);
collection.removeFirst();
collection.removeLast();
```

Konkretna kolekcja może być niemodyfikowalna albo nie obsługiwać danej
operacji. Wtedy operacja modyfikująca może rzucić
`UnsupportedOperationException`.

## 2. Dostęp do początku i końca

Przykład z listą:

```java
List<String> transactions = new ArrayList<>();
transactions.add("OPEN");
transactions.add("DEPOSIT");
transactions.add("WITHDRAW");

String oldest = transactions.getFirst();
String newest = transactions.getLast();
```

Wynik:

```text
oldest = OPEN
newest = WITHDRAW
```

W Java 21 metody `getFirst()` i `getLast()` są dostępne również bezpośrednio
na `List`.

Jeśli kolekcja jest pusta, `getFirst()` i `getLast()` rzucają
`NoSuchElementException`. Jeżeli pusty wynik jest normalnym przypadkiem
biznesowym, można wcześniej sprawdzić `isEmpty()` albo użyć innego API
odpowiedniego do danego przypadku.

## 3. Odwrócony widok przez `reversed()`

```java
SequencedCollection<String> newestFirst = transactions.reversed();
```

`reversed()` zwraca kolekcję przedstawioną w odwrotnej kolejności:

```text
transactions:  OPEN, DEPOSIT, WITHDRAW
newestFirst:   WITHDRAW, DEPOSIT, OPEN
```

Warto zapamiętać:

- `reversed()` zwraca widok, a niekoniecznie niezależną kopię,
- zmiany widoczne przez jedną stronę mogą być widoczne przez drugą,
- możliwość modyfikacji zależy od kolekcji źródłowej,
- widok odwraca kolejność, ale nie odwraca ani nie zmienia samych elementów.

Przykład:

```java
List<String> transactions = new ArrayList<>(
        List.of("OPEN", "DEPOSIT", "WITHDRAW")
);

SequencedCollection<String> reversed = transactions.reversed();

transactions.add("TRANSFER");

System.out.println(reversed);
```

Ponieważ jest to widok nad modyfikowalną listą, nowy element należy do tej
samej kolekcji i jest uwzględniony w jej odwróconym widoku.

Jeśli potrzebujesz niezależnej kopii, utwórz ją jawnie.

## 4. Odwracanie widoku a modyfikowanie listy

`reversed()` nie wykonuje klasycznego odwrócenia danych w miejscu:

```java
Collections.reverse(transactions);
```

Zamiast tego udostępnia widok z odwrotną kolejnością:

```java
SequencedCollection<Transaction> newestFirst = transactions.reversed();
```

Te operacje mają różne intencje:

| Operacja | Efekt |
|---|---|
| `transactions.reversed()` | Zwraca odwrócony widok |
| `Collections.reverse(transactions)` | Modyfikuje kolejność elementów listy |
| `new ArrayList<>(transactions.reversed())` | Tworzy nową listę w odwróconej kolejności |

## 5. `SequencedSet`

`SequencedSet` jest zbiorem, który zachowuje kolejność elementów.

Przykładem istniejącej kolekcji implementującej ten interfejs jest
`LinkedHashSet`:

```java
SequencedSet<String> transactionTypes = new LinkedHashSet<>();
transactionTypes.add("DEPOSIT");
transactionTypes.add("WITHDRAW");
transactionTypes.add("TRANSFER");

String firstType = transactionTypes.getFirst();
String lastType = transactionTypes.getLast();
```

W przeciwieństwie do zwykłego `Set`, kolejność iteracji `LinkedHashSet`
odzwierciedla kolejność wstawiania elementów.

## 6. `SequencedMap`

`SequencedMap` zachowuje kolejność wpisów mapy i udostępnia metody pracy
z pierwszym oraz ostatnim wpisem:

```java
SequencedMap<String, String> accountStatuses = new LinkedHashMap<>();
accountStatuses.put("PLN-1001", "ACTIVE");
accountStatuses.put("EUR-2001", "BLOCKED");

Map.Entry<String, String> first = accountStatuses.firstEntry();
Map.Entry<String, String> last = accountStatuses.lastEntry();
```

Dostępny jest także odwrócony widok:

```java
SequencedMap<String, String> reverseStatuses =
        accountStatuses.reversed();
```

`SequencedMap` udostępnia również operacje takie jak:

```java
accountStatuses.pollFirstEntry();
accountStatuses.pollLastEntry();
```

Metody `pollFirstEntry()` i `pollLastEntry()` usuwają odpowiedni wpis
i zwracają go, jeżeli kolekcja obsługuje modyfikacje.

## 7. Przykład z historią mini-banku

Załóżmy, że historia jest zapisana od najstarszej do najnowszej:

```java
List<Transaction> history = new ArrayList<>();
```

Pierwsza transakcja:

```java
Transaction oldest = history.getFirst();
```

Ostatnia transakcja:

```java
Transaction newest = history.getLast();
```

Historia wyświetlona od najnowszej:

```java
SequencedCollection<Transaction> newestFirst = history.reversed();
```

Możemy też przekazać taki widok do kodu, który ma iterować od najnowszych
zdarzeń, bez zmieniania podstawowego porządku listy.

## 8. Puste kolekcje

Te metody wymagają obecności elementu:

```java
transactions.getFirst();
transactions.getLast();
```

Jeśli lista jest pusta, zostanie rzucony `NoSuchElementException`.

Przykład jawnej obsługi:

```java
if (transactions.isEmpty()) {
    return "NO TRANSACTIONS";
}

return transactions.getLast().description();
```

Nie należy zakładać, że `getFirst()` lub `getLast()` zwróci `null`.

## 9. Testowanie w Spocku

Testy powinny obejmować:

- pierwszy element,
- ostatni element,
- kolejność odwróconego widoku,
- zachowanie przy pustej kolekcji,
- zależność widoku od zmian kolekcji źródłowej,
- brak zmiany kolejności oryginalnej listy przy samym `reversed()`.

Przykładowy kształt specyfikacji:

```groovy
def "reversed view returns transactions newest first"() {
    given:
    def transactions = ["OPEN", "DEPOSIT", "WITHDRAW"]

    when:
    def reversed = transactions.reversed()

    then:
    reversed.toList() == ["WITHDRAW", "DEPOSIT", "OPEN"]
    transactions == ["OPEN", "DEPOSIT", "WITHDRAW"]
}
```

## 10. Typowe błędy i nieporozumienia

### Traktowanie `reversed()` jak kopii

`reversed()` udostępnia odwrócony widok. Nie należy zakładać, że powstaje
niezależna kopia.

### Oczekiwanie `null` dla pustej listy

`getFirst()` i `getLast()` rzucają `NoSuchElementException`, gdy lista jest
pusta.

### Zakładanie, że każda kolekcja pozwala na modyfikacje

Kolekcja zwrócona przez `List.of(...)` jest niemodyfikowalna. Operacje
modyfikujące mogą rzucić `UnsupportedOperationException`.

### Mylenie odwróconego widoku z mutacją

```java
transactions.reversed();
```

nie jest tym samym co:

```java
Collections.reverse(transactions);
```

Pierwsze tworzy odwrócony widok, drugie zmienia kolejność listy.

## 11. Pytania rekrutacyjne

### Co to jest `SequencedCollection`?

To interfejs kolekcji, która ma określoną kolejność napotykania elementów
i udostępnia wspólne operacje na początku oraz końcu sekwencji.

### Jak pobrać pierwszy i ostatni element listy w Java 21?

```java
list.getFirst();
list.getLast();
```

### Co zwraca `reversed()`?

Odwrócony widok sekwencji. Nie należy automatycznie zakładać, że jest to
niezależna kopia.

### Czy `reversed()` zmienia oryginalną listę?

Samo wywołanie nie odwraca elementów listy źródłowej. Zwraca widok
z przeciwną kolejnością.

### Jak obsłużyć pustą kolekcję?

Sprawdzić `isEmpty()` przed `getFirst()` lub `getLast()`, albo użyć API,
które odpowiada kontraktowi danego zastosowania.

### Jakie typy interfejsów dodano w Java 21?

```text
SequencedCollection
SequencedSet
SequencedMap
```

## 12. Ćwiczenia

1. Utwórz listę transakcji w kolejności od najstarszej do najnowszej.
2. Pobierz pierwszą i ostatnią transakcję przez `getFirst()` i `getLast()`.
3. Utwórz odwrócony widok przez `reversed()`.
4. Sprawdź, że iterowanie po widoku nie zmieniło listy źródłowej.
5. Dodaj element do listy źródłowej i sprawdź, co widać w widoku.
6. Sprawdź zachowanie `getFirst()` dla pustej listy.
7. Użyj `LinkedHashSet` jako `SequencedSet`.
8. Użyj `LinkedHashMap` jako `SequencedMap` i pobierz pierwszy oraz ostatni wpis.

## Uruchomienie testu

Po utworzeniu klas i speca uruchom:

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.lab.java21.collections.SequencedCollectionsSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.lab.java21.collections.SequencedCollectionsSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

Java 21 wprowadziła wspólne kontrakty dla kolekcji o określonej kolejności:

```text
SequencedCollection
SequencedSet
SequencedMap
```

Najważniejsze operacje:

```java
getFirst()
getLast()
reversed()
```

Przy pracy z `reversed()` pamiętaj, że otrzymujesz odwrócony widok. Zwrócony
widok, możliwość modyfikacji i zachowanie zmian zależą od kolekcji źródłowej.
