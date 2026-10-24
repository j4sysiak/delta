# Lab 20 — Ograniczanie równoległych wywołań przez `Semaphore`

## Cel

Virtual threads pozwalają uruchomić wiele zadań oczekujących na I/O, ale
zewnętrzny serwis, baza danych lub API mogą mieć limit równoczesnych
wywołań. W tym labie skupiamy się tylko na jednym rozwiązaniu:
`Semaphore`.

Na razie pomijamy pinning, `ThreadLocal` i diagnostykę JVM. To osobne,
bardziej zaawansowane tematy. Nie zmieniamy kodu produkcyjnego mini-banku.

## 1. Problem

Załóżmy, że dostawca wyciągów pozwala na maksymalnie dwa równoczesne
żądania. Virtual threads same nie narzucą tego limitu:

```text
dużo zadań aplikacji
        ↓
zewnętrzny serwis obsługuje maksymalnie 2 naraz
```

Musimy sprawić, żeby pozostałe zadania poczekały, zanim wywołają serwis.

## 2. Jak działa `Semaphore`?

Semafor przechowuje liczbę pozwoleń (permits):

```java
var semaphore = new Semaphore(2);
```

- `acquire()` pobiera jedno pozwolenie; jeśli żadne nie jest dostępne,
  wątek czeka.
- `release()` oddaje pozwolenie.
- `acquire()` jest przerywalne i może zgłosić `InterruptedException`.

Najważniejszy wzorzec:

```java
semaphore.acquire();
try {
    return callRemoteService();
} finally {
    semaphore.release();
}
```

`finally` jest kluczowe: permit wraca również wtedy, gdy wywołanie rzuci
wyjątek. Bez tego po kilku błędach semafor mógłby stracić wszystkie
pozwolenia, a kolejne zadania czekałyby bez końca.

## 3. Implementacja limitera do wklejenia

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\virtualthreads\BankCallLimiter.java
```

Wklej:

```java
package com.delta.bank.lab.java21.virtualthreads;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class BankCallLimiter {

    private final Semaphore semaphore;

    public BankCallLimiter(int maxConcurrentCalls) {
        if (maxConcurrentCalls <= 0) {
            throw new IllegalArgumentException(
                    "maxConcurrentCalls must be positive"
            );
        }
        this.semaphore = new Semaphore(maxConcurrentCalls);
    }

    public <T> T execute(Callable<T> operation) throws Exception {
        Objects.requireNonNull(operation, "operation must not be null");

        semaphore.acquire();
        try {
            return operation.call();
        } finally {
            semaphore.release();
        }
    }
}
```

Przebieg `execute(...)`:

```text
sprawdź operację
    ↓
acquire() — poczekaj na permit
    ↓
wykonaj wywołanie
    ↓
release() w finally
```

Metoda propaguje wyjątki operacji i `InterruptedException` z `acquire()`.
Nie połyka błędów ani sygnału przerwania.

## 4. Użycie z virtual threads

Ten sam limiter może chronić klienta z Lab3:

```java
var limiter = new BankCallLimiter(2);

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    var first = executor.submit(() ->
            limiter.execute(() -> loader.loadStatement("PLN-1001"))
    );
    var second = executor.submit(() ->
            limiter.execute(() -> loader.loadStatement("PLN-1002"))
    );

    var firstResult = first.get();
    var secondResult = second.get();
}
```

Potrzebne importy w przykładzie:

```java
import java.util.concurrent.Executors;
```

Kod loadera i nazwa metody są schematyczne — nie wklejaj tego przykładu
bezpośrednio do istniejącej klasy, jeśli nie ma tam `loadStatement`.
Główna zasada: wszystkie zadania, które mają dzielić limit, muszą korzystać
z tej samej instancji `BankCallLimiter`.

`Semaphore(2)` ogranicza do dwóch operacji wewnątrz sekcji chronionej.
Nie jest to automatycznie globalny limit dla całej aplikacji, jeśli każda
część utworzy własny limiter.

## 5. Testy Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\virtualthreads\BankCallLimiterSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.virtualthreads

import spock.lang.Specification

import java.util.concurrent.Callable

class BankCallLimiterSpec extends Specification {

    def "returns operation result"() {
        given:
        def limiter = new BankCallLimiter(2)

        when:
        def result = limiter.execute(
                { "statement loaded" } as Callable<String>
        )

        then:
        result == "statement loaded"
    }

    def "releases permit when operation throws"() {
        given:
        def limiter = new BankCallLimiter(1)

        when:
        limiter.execute({
            throw new IllegalStateException("remote service failed")
        } as Callable<String>)

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "remote service failed"

        when:
        def result = limiter.execute(
                { "next call succeeded" } as Callable<String>
        )

        then:
        result == "next call succeeded"
    }

    def "rejects a non-positive limit"() {
        when:
        new BankCallLimiter(0)

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "maxConcurrentCalls must be positive"
    }

    def "rejects a null operation"() {
        given:
        def limiter = new BankCallLimiter(1)

        when:
        limiter.execute(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "operation must not be null"
    }
}
```

Test po wyjątku sprawdza najważniejszy szczegół implementacyjny:
nieudane wywołanie nie zatrzymuje permitu na zawsze.

Nie sprawdzamy równoległości przez `sleep` ani pomiar czasu. Najpierw
opanowujemy kontrakt semafora; deterministyczny test współbieżności byłby
osobnym, kolejnym ćwiczeniem.

## 6. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.virtualthreads.BankCallLimiterSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.virtualthreads.BankCallLimiterSpec"
```

Test nie wymaga Springa, bazy ani sieci.

## 7. Co ogranicza limiter?

Limiter ogranicza liczbę operacji, które jednocześnie weszły do
`execute(...)` na tej samej instancji semafora. Nie ogranicza:

- całkowitej liczby zadań oczekujących,
- liczby virtual threads w całej aplikacji,
- ruchu omijającego ten limiter,
- połączeń używanych przez inne instancje limitera.

Przy rzeczywistym API trzeba też rozważyć timeout, limit żądań dostawcy,
backpressure i zachowanie po przerwaniu. Sam semafor nie rozwiązuje
wszystkich problemów odporności systemu.

## 8. Pytania kontrolne

1. Co oznacza liczba przekazana do `new Semaphore(2)`?
2. Co robi `acquire()`, gdy nie ma dostępnych pozwoleń?
3. Dlaczego `release()` znajduje się w `finally`?
4. Co się stanie, jeśli operacja rzuci wyjątek?
5. Dlaczego wszystkie zadania muszą używać tej samej instancji limitera?
6. Czy limiter ogranicza liczbę zadań oczekujących?
7. Czy virtual threads same ograniczają ruch do zewnętrznego API?

Do zapamiętania:

```text
virtual threads ułatwiają obsługę wielu blokujących zadań
Semaphore ogranicza liczbę operacji wykonywanych jednocześnie
release() w finally zapobiega utracie permitu po wyjątku
```
