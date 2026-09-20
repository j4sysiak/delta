# Lab 4 — Java 21: współbieżność i virtual threads

## Cel laba

W Lab 3 używaliśmy virtual threads do równoległego wykonywania niezależnych
zadań. W tym labie sprawdzamy, co się dzieje, gdy wiele takich zadań modyfikuje
ten sam wspólny stan.

Zbudujemy mały eksperyment bankowy:

```text
saldo początkowe
        ↓
wiele równoległych wpłat
        ↓
saldo końcowe
```

Przykład:

```text
saldo początkowe: 0.00
liczba wpłat:     1000
kwota wpłaty:     1.00
oczekiwane saldo: 1000.00
```

Najważniejsza lekcja:

```text
virtual threads nie zapewniają automatycznie bezpieczeństwa wspólnego stanu
```

Virtual threads rozwiązują problem lekkiego uruchamiania wielu zadań, ale nie
sprawiają, że operacje na wspólnych danych stają się atomowe.

## Pliki laba

### Kod produkcyjny

```text
src/main/java/com/delta/bank/lab/java21/concurrency/UnsafeBankAccount.java
src/main/java/com/delta/bank/lab/java21/concurrency/ConcurrentBankAccount.java
src/main/java/com/delta/bank/lab/java21/concurrency/VirtualThreadDepositRunner.java
```

### Test Spock

```text
src/test/groovy/com/delta/bank/ConcurrentVirtualThreadSpec.groovy
```

## 1. Race condition

Race condition występuje wtedy, gdy wynik programu zależy od kolejności
wykonywania równoległych operacji.

Pozornie prosta operacja:

```java
balance = balance.add(amount);
```

nie musi być atomowa. W praktyce składa się z kilku kroków:

```text
1. odczytaj aktualne balance
2. oblicz balance + amount
3. zapisz nową wartość
```

Dwa wątki mogą wykonać te kroki w następującej kolejności:

```text
Wątek A: odczytuje 0.00
Wątek B: odczytuje 0.00
Wątek A: oblicza 1.00
Wątek B: oblicza 1.00
Wątek A: zapisuje 1.00
Wątek B: zapisuje 1.00
```

Po dwóch wpłatach saldo wynosi `1.00`, chociaż powinno wynosić `2.00`.
Aktualizacja wątku A została utracona.

## 2. Dlaczego `BigDecimal` nie wystarcza

`BigDecimal` jest niemutowalny i dobrze nadaje się do reprezentowania kwot.
Nie oznacza to jednak, że operacja przypisania jest bezpieczna
współbieżnie:

```java
balance = balance.add(amount);
```

Niemutowalność wartości i bezpieczeństwo całej sekwencji odczyt-obliczenie-zapis
to dwa różne zagadnienia.

## 3. Wersja niebezpieczna

Klasa bez synchronizacji może tracić aktualizacje:

```java
public class UnsafeBankAccount {

    private BigDecimal balance = BigDecimal.ZERO;

    public void deposit(BigDecimal amount) {
        BigDecimal currentBalance = balance;
        Thread.yield();
        balance = currentBalance.add(amount);
    }

    public BigDecimal balance() {
        return balance;
    }
}
```

`Thread.yield()` w przykładzie ma charakter edukacyjny. Zwiększa szansę
przełączenia wątku pomiędzy odczytem a zapisem. Nie jest mechanizmem
synchronizacji i nie powinien być używany jako rozwiązanie problemu.

## 4. Wersja bezpieczna z `synchronized`

Najprostsze zabezpieczenie:

```java
public synchronized void deposit(BigDecimal amount) {
    balance = balance.add(amount);
}
```

Słowo kluczowe `synchronized` gwarantuje, że w danym momencie tylko jeden
wątek wykona sekcję krytyczną dla tego obiektu.

W rezultacie operacja:

```java
balance = balance.add(amount);
```

jest wykonywana jako jedna bezpieczna sekcja z punktu widzenia innych wątków.

## 5. `AtomicReference<BigDecimal>`

`BigDecimal` nie ma atomowej metody `increment`. Można jednak przechowywać go
w `AtomicReference` i wykonać atomową aktualizację funkcją:

```java
balance.updateAndGet(current -> current.add(amount));
```

JVM może ponowić funkcję, jeżeli inny wątek zmieni wartość pomiędzy odczytem
a zapisem. Funkcja przekazana do `updateAndGet` powinna być krótka i nie
powinna wywoływać efektów ubocznych.

## 6. `ReentrantLock`

Alternatywą dla `synchronized` jest jawny lock:

```java
lock.lock();
try {
    balance = balance.add(amount);
} finally {
    lock.unlock();
}
```

Blok `finally` jest konieczny. Gwarantuje zwolnienie blokady także wtedy,
gdy operacja zakończy się wyjątkiem.

`ReentrantLock` daje dodatkowe możliwości, np.:

- `tryLock()`,
- przerwanie oczekiwania na lock,
- sprawdzanie stanu blokady.

W tym labie zaczynamy od `synchronized`, ponieważ jest najprostsze do
zrozumienia.

## 7. Virtual threads a wspólny stan

Runner uruchamia wiele wpłat:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int index = 0; index < deposits; index++) {
        executor.submit(() -> account.deposit(amount));
    }
}
```

Executor virtual threads ułatwia uruchomienie dużej liczby zadań, ale nie
decyduje, czy metoda `deposit` jest bezpieczna.

Bezpieczeństwo musi znajdować się w klasie, która chroni wspólny stan:

```text
VirtualThreadDepositRunner
        ↓
ConcurrentBankAccount.deposit(...)
        ↓
bezpieczna aktualizacja balance
```

## 8. Czekanie na zakończenie zadań

Samo przesłanie zadań do executora nie oznacza, że wszystkie zakończyły się
przed odczytem salda.

Można użyć `Future`:

```java
List<Future<?>> futures = new ArrayList<>();

for (...) {
    futures.add(executor.submit(() -> account.deposit(amount)));
}

for (Future<?> future : futures) {
    future.get();
}
```

Dopiero po odebraniu wszystkich wyników można sprawdzić saldo końcowe.

W Java 21 można też rozważyć `StructuredTaskScope`, ale ten lab używa
standardowego `ExecutorService` i `Future`, aby połączyć temat
współbieżności z mechanizmem poznanym w Lab 3.

## 9. Test współbieżności w Spocku

Test powinien:

1. utworzyć konto,
2. uruchomić wiele równoległych wpłat,
3. poczekać na zakończenie wszystkich zadań,
4. sprawdzić saldo końcowe.

Przykład oczekiwania:

```groovy
account.balance() == new BigDecimal("1000.00")
```

Nie należy sprawdzać dokładnego czasu wykonania jako głównej asercji.
Czas zależy od komputera, systemu operacyjnego, Gradle i obciążenia środowiska.

## 10. Deterministyczność testu

Test race condition może czasami przejść, a czasami nie, jeżeli nie zwiększymy
szansy wystąpienia przełączenia pomiędzy odczytem i zapisem.

W tym labie `Thread.yield()` służy tylko do demonstracji problemu. Nie jest
gwarancją reprodukcji race condition.

Dla wersji bezpiecznej test powinien być stabilny:

```text
każda wpłata jest wykonana
saldo końcowe zawsze jest prawidłowe
```

W testach produkcyjnego kodu nie należy polegać wyłącznie na losowym
przełączaniu wątków.

## 11. Typowe błędy

### Mylenie virtual threads z synchronizacją

Niepoprawne założenie:

```text
virtual threads = thread safety
```

Poprawne rozumienie:

```text
virtual threads = lekki sposób wykonywania wielu zadań
thread safety = ochrona współdzielonego stanu
```

### Używanie `volatile` jako zamiennika atomowości

`volatile` zapewnia widoczność wartości między wątkami, ale nie zabezpiecza
całej operacji:

```java
volatile BigDecimal balance;
balance = balance.add(amount);
```

To nadal może prowadzić do utraty aktualizacji.

### Odczyt salda przed zakończeniem zadań

Nie wolno sprawdzać salda, zanim wszystkie `Future` nie zostaną zakończone.

### Brak `finally` przy `ReentrantLock`

Lock może pozostać zablokowany, jeżeli wyjątek wystąpi przed `unlock()`.

### Wspólny mutowalny stan w teście

Test powinien jasno kontrolować, które obiekty są współdzielone. W przeciwnym
razie trudno ustalić, czy błąd pochodzi z kodu, czy z niepoprawnie zbudowanego
testu.

## 12. Pytania rekrutacyjne

### Czy virtual threads rozwiązują race condition?

Nie. Ułatwiają uruchomienie wielu zadań, ale wspólny stan nadal wymaga
synchronizacji lub atomowych struktur danych.

### Dlaczego `balance = balance.add(amount)` nie jest atomowe?

Ponieważ składa się z odczytu bieżącej wartości, obliczenia nowej wartości
i zapisu. Inny wątek może wykonać własną operację pomiędzy tymi krokami.

### Co robi `synchronized`?

Zapewnia wzajemne wykluczanie i widoczność zmian dla sekcji chronionej
konkretnym monitorem.

### Czy `volatile` wystarcza?

Nie, jeżeli operacja składa się z kilku kroków. `volatile` nie zapewnia
atomowości operacji read-modify-write.

### Kiedy użyć `AtomicReference`?

Gdy aktualizacja może być opisana jako krótka funkcja atomowa, a użycie
blokady nie jest potrzebne albo nie jest pożądane.

### Kiedy użyć `ReentrantLock`?

Gdy potrzebujesz funkcji dostępnych poza `synchronized`, np. `tryLock`,
przerywalnego oczekiwania albo bardziej elastycznego zarządzania blokadą.

### Czy `synchronized` jest niebezpieczne z virtual threads?

Nie należy automatycznie traktować go jako zakazanego. Trzeba jednak uważać
na długie sekcje krytyczne i blokowanie zasobów. Sekcja synchronizowana
powinna być krótka.

## 13. Ćwiczenia

1. Uruchom 1000 wpłat po `1.00`.
2. Sprawdź różnicę między `UnsafeBankAccount` i
   `ConcurrentBankAccount`.
3. Zastąp `synchronized` przez `AtomicReference<BigDecimal>`.
4. Dodaj wersję z `ReentrantLock`.
5. Dodaj metodę `withdraw`.
6. Zabezpiecz wpłatę i wypłatę przed ujemnym saldem.
7. Sprawdź, czy transfer wymaga atomowej ochrony dwóch kont.
8. Porównaj zachowanie przy 10, 100 i 1000 zadaniach.

## 14. Uruchomienie testu

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.ConcurrentVirtualThreadSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.ConcurrentVirtualThreadSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

W tym labie poznajemy:

```text
race condition
synchronized
AtomicReference<BigDecimal>
ReentrantLock
volatile a atomowość
Future i oczekiwanie na zadania
virtual threads ze wspólnym stanem
testowanie współbieżności w Spocku
```

Najważniejsza zasada:

```text
virtual threads uruchamiają zadania,
ale synchronizacja chroni dane.
```
