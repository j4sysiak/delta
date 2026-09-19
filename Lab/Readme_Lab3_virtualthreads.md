# Lab 3 — Java 21: Virtual Threads

## Cel laba

W tym labie uczymy się wirtualnych wątków wprowadzonych w Java 21.

Zbudujemy niezależny komponent szkoleniowy, który równolegle pobiera trzy
rodzaje danych bankowych:

- saldo konta,
- historię transakcji,
- podsumowanie konta.

Nie zmieniamy istniejącego `BankAccountService`. Celem jest poznanie
mechanizmu virtual threads w małym, kontrolowanym przykładzie.

## Pliki laba

### Kod produkcyjny

```text
src/main/java/com/delta/bank/lab/java21/virtualthreads/BankDataLoader.java
src/main/java/com/delta/bank/lab/java21/virtualthreads/VirtualThreadBankService.java
src/main/java/com/delta/bank/lab/java21/virtualthreads/BankDataResult.java
```

### Test Spock

```text
src/test/groovy/com/delta/bank/VirtualThreadBankSpec.groovy
```

## 1. Czym jest virtual thread?

Virtual thread to lekki wątek zarządzany przez JVM, a nie bezpośrednio
reprezentowany przez osobny ciężki wątek systemu operacyjnego.

W Java 21 można tworzyć bardzo dużą liczbę virtual threads, szczególnie dla
zadań, które większość czasu oczekują na I/O:

- odpowiedź HTTP,
- zapytanie do bazy danych,
- odczyt pliku,
- komunikację z zewnętrznym serwisem.

Virtual threads nie oznaczają, że każda operacja będzie wykonywana szybciej.
Ich główną zaletą jest możliwość obsługi wielu równoległych zadań
blokujących bez zajmowania dużej liczby ciężkich wątków platformowych.

## 2. Klasyczne wątki platformowe a virtual threads

Klasyczny executor:

```java
try (var executor = Executors.newFixedThreadPool(10)) {
    Future<String> result = executor.submit(() -> "bank data");
    return result.get();
}
```

Executor dla virtual threads:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> result = executor.submit(() -> "bank data");
    return result.get();
}
```

Najważniejsza różnica znajduje się tutaj:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

Ten executor tworzy nowy virtual thread dla każdego przesłanego zadania.

## 3. Podstawowy przykład

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> result = executor.submit(() -> {
        Thread.sleep(100);
        return "bank data";
    });

    String value = result.get();
}
```

Ważne elementy:

1. `submit(...)` przesyła zadanie do wykonania.
2. Zadanie zwraca `Future`.
3. `Future.get()` czeka na wynik.
4. `try-with-resources` zamyka executor.

Metoda `Thread.sleep(...)` może rzucić `InterruptedException`, dlatego
rzeczywista implementacja musi poprawnie obsłużyć przerwanie wątku.

## 4. `BankDataLoader`

`BankDataLoader` będzie reprezentował źródło danych bankowych.

Przykładowe metody:

```java
String loadAccountBalance(String accountNumber);

String loadTransactionHistory(String accountNumber);

String loadAccountSummary(String accountNumber);
```

W celach edukacyjnych metody mogą symulować opóźnienie. W prawdziwej aplikacji
ich rolę mogłyby pełnić wywołania repozytorium, REST API albo innego serwisu.

## 5. Równoległe pobieranie danych

`VirtualThreadBankService` uruchomi trzy niezależne operacje:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> balance = executor.submit(
            () -> loader.loadAccountBalance(accountNumber)
    );

    Future<String> history = executor.submit(
            () -> loader.loadTransactionHistory(accountNumber)
    );

    Future<String> summary = executor.submit(
            () -> loader.loadAccountSummary(accountNumber)
    );

    return new BankDataResult(
            balance.get(),
            history.get(),
            summary.get()
    );
}
```

Operacje są niezależne, więc mogą być wykonywane równolegle. Końcowy wynik
powstaje dopiero po odebraniu wszystkich trzech wartości.

## 6. `Future`

`Future<T>` reprezentuje wynik zadania, który może być dostępny dopiero
w przyszłości.

```java
Future<String> future = executor.submit(() -> "result");
String result = future.get();
```

Najważniejsze metody:

```java
future.get();
future.isDone();
future.cancel(true);
```

`get()` jest operacją blokującą. Jeżeli zadanie jeszcze się nie zakończyło,
wątek wywołujący czeka na jego wynik.

## 7. Zamykanie executora

Executor powinien być zamykany:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // zadania
}
```

`try-with-resources` zapewnia zamknięcie executora również wtedy, gdy podczas
wykonywania zadania wystąpi wyjątek.

Nie należy zostawiać executorów otwartych, ponieważ może to prowadzić do
wycieków zasobów i problemów z kończeniem aplikacji.

## 8. Obsługa wyjątków

Jeżeli zadanie rzuć wyjątek, wywołanie `Future.get()` może rzucić
`ExecutionException`.

Przykład:

```java
try {
    return future.get();
} catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("Thread was interrupted", exception);
} catch (ExecutionException exception) {
    throw new IllegalStateException("Bank data loading failed", exception.getCause());
}
```

### Dlaczego przywracamy status przerwania?

`InterruptedException` informuje, że ktoś zażądał przerwania bieżącego wątku.
Po przechwyceniu wyjątku przywracamy flagę przerwania:

```java
Thread.currentThread().interrupt();
```

Dzięki temu kod wyżej w stosie nadal może zauważyć, że wątek został
przerwany.

## 9. Virtual threads a operacje I/O

Virtual threads dobrze pasują do zadań takich jak:

```text
virtual thread
    └── oczekiwanie na odpowiedź HTTP
virtual thread
    └── oczekiwanie na bazę danych
virtual thread
    └── oczekiwanie na plik
```

Podczas oczekiwania JVM może wykorzystać zasoby platformowego wątku do
innego zadania.

Virtual threads nie zwiększają automatycznie wydajności obliczeń CPU-heavy.
Jeżeli zadanie przez cały czas intensywnie wykonuje obliczenia, potrzebna
liczba procesorów nadal pozostaje ograniczeniem.

## 10. Spock w tym labie

Testy Spocka powinny sprawdzać zachowanie komponentu, a nie implementację
JVM.

Przykładowe obszary testów:

- wynik zawiera saldo, historię i podsumowanie,
- wszystkie trzy metody loadera zostały użyte,
- wyjątek z loadera jest propagowany,
- przerwanie wątku nie jest ignorowane,
- wiele zadań może zostać wykonanych niezależnie.

Przykładowy schemat:

```groovy
given:
def loader = Mock(BankDataLoader)
def service = new VirtualThreadBankService(loader)

and:
1 * loader.loadAccountBalance("PLN-1001") >> "BALANCE"
1 * loader.loadTransactionHistory("PLN-1001") >> "HISTORY"
1 * loader.loadAccountSummary("PLN-1001") >> "SUMMARY"

when:
def result = service.loadAll("PLN-1001")

then:
result.balance() == "BALANCE"
result.history() == "HISTORY"
result.summary() == "SUMMARY"
```

## 11. Nie testuj czasu przez przypadek

Jeżeli loader symuluje opóźnienie przez `Thread.sleep`, test może stać się
niestabilny, gdy będzie sprawdzał dokładny czas wykonania.

Nie należy pisać testu zależnego od precyzyjnych wartości:

```groovy
result.durationInMillis() == 100
```

Lepsze jest sprawdzenie wyniku biznesowego:

```groovy
result.balance() == "BALANCE"
result.history() == "HISTORY"
result.summary() == "SUMMARY"
```

Test współbieżności powinien być deterministyczny i nie powinien wymagać,
aby system zawsze kończył pracę w konkretnej liczbie milisekund.

## 12. Typowe błędy

### Brak zamknięcia executora

Zły wzorzec:

```java
var executor = Executors.newVirtualThreadPerTaskExecutor();
```

Lepszy wzorzec:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // zadania
}
```

### Ignorowanie `InterruptedException`

Nie należy robić:

```java
catch (InterruptedException exception) {
    // nic
}
```

Należy przywrócić status przerwania albo jawnie propagować błąd:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("Thread was interrupted", exception);
}
```

### Oczekiwanie szybszego CPU

Virtual threads nie są zamiennikiem optymalizacji algorytmów. Ich główne
zastosowanie to duża liczba lekkich zadań oczekujących na I/O.

### Zbyt duży zakres testu

W tym labie testujemy kontrolowany komponent szkoleniowy. Nie zmieniamy
całego request handlingu aplikacji i nie przerabiamy wszystkich serwisów
mini-banku na virtual threads.

## 13. Pytania rekrutacyjne

### Czym jest virtual thread?

To lekki wątek zarządzany przez JVM, przeznaczony szczególnie do dużej liczby
zadań, które często blokują się na operacjach I/O.

### Jak utworzyć executor virtual threads?

```java
Executors.newVirtualThreadPerTaskExecutor()
```

### Czy virtual threads zastępują wszystkie klasyczne wątki?

Nie. Dobór zależy od charakteru zadania. Virtual threads są bardzo dobre dla
wielu blokujących operacji I/O, ale nie rozwiązują ograniczeń obliczeń CPU.

### Czy virtual thread jest zawsze szybszy?

Nie. Zwykle daje lepszą skalowalność i mniejsze zużycie zasobów przy dużej
liczbie zadań blokujących, ale pojedyncze zadanie nie musi wykonać się
szybciej.

### Co zwraca `submit(...)`?

Zwraca `Future`, przez który można odebrać wynik, sprawdzić stan zadania albo
je anulować.

### Dlaczego używamy `try-with-resources`?

Żeby executor został zamknięty po zakończeniu pracy, także w przypadku
wyjątku.

### Co zrobić z `InterruptedException`?

Przywrócić status przerwania:

```java
Thread.currentThread().interrupt();
```

Następnie należy propagować błąd lub zakończyć operację w sposób zgodny
z kontraktem metody.

## 14. Ćwiczenia

1. Uruchom trzy zadania przez
   `Executors.newVirtualThreadPerTaskExecutor()`.
2. Zwróć ich wyniki w jednym rekordzie `BankDataResult`.
3. Dodaj czwarte zadanie pobierające status konta.
4. Dodaj test, w którym jedno zadanie rzuca wyjątek.
5. Sprawdź, czy pozostałe zadania są poprawnie obsługiwane.
6. Dodaj logowanie:

   ```java
   Thread.currentThread()
   ```

7. Porównaj nazwę i właściwości wątku platformowego oraz virtual thread.

## 15. Uruchomienie testu

Po utworzeniu klas i testu uruchom:

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.VirtualThreadBankSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.VirtualThreadBankSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

W tym labie poznajemy:

```text
Executors.newVirtualThreadPerTaskExecutor()
Future
try-with-resources
obsługę InterruptedException
obsługę ExecutionException
równoległe zadania I/O
testowanie współbieżnego komponentu w Spocku
```

Najważniejszy wzorzec:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> result = executor.submit(() -> "bank data");
    return result.get();
}
```

Na rozmowie technicznej warto podkreślić, że virtual threads poprawiają
skalowalność dużej liczby zadań blokujących, ale nie sprawiają automatycznie,
że obliczenia CPU wykonują się szybciej.
