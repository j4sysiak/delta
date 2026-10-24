# Lab 21 — Test limitu `Semaphore` z virtual threads

## Cel

W Lab20 ograniczyłeś liczbę równoczesnych wywołań przez `Semaphore`.
Teraz sprawdzimy, czy limiter naprawdę wpuszcza najwyżej określoną liczbę
operacji jednocześnie.

Uruchomimy kilka zadań na virtual threads i użyjemy `CountDownLatch`,
żeby sterować ich przebiegiem. Test nie będzie polegał na `Thread.sleep()`
ani na mierzeniu czasu trwania zadań.

Zakładamy, że masz już klasę `BankCallLimiter` z Lab20:

```text
src\main\java\com\delta\bank\lab\java21\virtualthreads\BankCallLimiter.java
```

Nie twórz jej drugi raz i nie zmieniaj kodu produkcyjnego mini-banku.

## 1. Kontrakt, który sprawdzamy

Utworzymy limiter z dwoma permitami i uruchomimy cztery zadania:

```text
zadanie A ─┐
zadanie B ─┴─ dwa wchodzą do operacji
zadanie C ─── czeka na permit
zadanie D ─── czeka na permit
```

Test poczeka, aż dwa pierwsze zadania wejdą do chronionej operacji.
Następnie sprawdzi, że w tym samym czasie aktywne są dokładnie dwa zadania,
a kolejne nie weszły jeszcze do operacji.

Po zwolnieniu zadań oczekujących w teście, pozostałe dwa mają wejść
i wszystkie cztery powinny zakończyć się poprawnie.

## 2. Dlaczego `CountDownLatch` zamiast `Thread.sleep()`?

`Thread.sleep(100)` nie gwarantuje, że inne zadanie zdążyło się uruchomić.
Wynik takiego testu zależałby od obciążenia maszyny i schedulera.

`CountDownLatch` synchronizuje konkretne zdarzenia:

- `enteredFirstPair` informuje test, że dwa zadania weszły do sekcji,
- `releaseFirstPair` pozwala tym zadaniom kontynuować,
- `allCompleted` informuje, że wszystkie zadania zakończyły się.

Limity czasu w `await(...)` są tylko zabezpieczeniem, by błędny test
nie zawiesił się bez końca. Nie służą do mierzenia wydajności.

## 3. Test Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\virtualthreads\BankCallLimiterConcurrencySpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.virtualthreads

import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BankCallLimiterConcurrencySpec extends Specification {

    def "never allows more than the configured number of concurrent calls"() {
        given:
        int limit = 2
        int taskCount = 4
        def limiter = new BankCallLimiter(limit)
        def activeCalls = new AtomicInteger()
        def maximumActiveCalls = new AtomicInteger()
        def enteredFirstPair = new CountDownLatch(limit)
        def releaseFirstPair = new CountDownLatch(1)
        def         allCompleted = new CountDownLatch(taskCount)

        when:
        def results = []
        def executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            def futures = (1..taskCount).collect { taskNumber ->
                executor.submit({
                    try {
                        return limiter.execute({
                            int current = activeCalls.incrementAndGet()
                            maximumActiveCalls.accumulateAndGet(
                                    current,
                                    Math::max
                            )
                            enteredFirstPair.countDown()

                            try {
                                if (!releaseFirstPair.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException(
                                            "test did not release first tasks"
                                    )
                                }
                                return "result-${taskNumber}"
                            } finally {
                                activeCalls.decrementAndGet()
                            }
                        } as Callable<String>)
                    } finally {
                        allCompleted.countDown()
                    }
                } as Callable<String>)
            }

            assert enteredFirstPair.await(5, TimeUnit.SECONDS)
            assert activeCalls.get() == limit
            assert maximumActiveCalls.get() == limit

            releaseFirstPair.countDown()

            results = futures.collect { it.get(5, TimeUnit.SECONDS) }
        } finally {
            releaseFirstPair.countDown()
            executor.close()
        }

        then:
        allCompleted.count == 0
        maximumActiveCalls.get() == limit
        activeCalls.get() == 0
        results.toSet() == ["result-1", "result-2", "result-3", "result-4"].toSet()
    }
}
```

Dodaj import `AtomicReference` na górze speca razem z pozostałymi importami:

```groovy
import java.util.concurrent.atomic.AtomicReference
```

`AtomicInteger` zlicza aktywne wywołania bez wyścigu danych. `finally`
zmniejsza licznik niezależnie od tego, czy operacja zakończyła się
sukcesem, czy wyjątkiem.

## 6. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.virtualthreads.BankCallLimiterConcurrencySpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.virtualthreads.BankCallLimiterConcurrencySpec"
```

Test nie wymaga Springa, bazy ani sieci. Virtual threads są finalne
w Java 21 i nie wymagają `--enable-preview`.

## 7. Jak czytać asercje?

```groovy
assert enteredFirstPair.await(5, TimeUnit.SECONDS)
```

Test czeka do pięciu sekund, aż dwa zadania wejdą do chronionej operacji.

```groovy
assert activeCalls.get() == limit
```

W tym kontrolowanym momencie oba pozwolenia są zajęte.

```groovy
releaseFirstPair.countDown()
```

Test pozwala pierwszym dwóm zadaniom zakończyć pracę, dzięki czemu
oczekujące zadania mogą zdobyć permit.

```groovy
maximumActiveCalls.get() == limit
```

W całym teście nigdy nie było więcej aktywnych wywołań niż skonfigurowany
limit.

## 8. Dlaczego nie sprawdzamy, które konkretnie zadania wejdą pierwsze?

`Semaphore` nie jest użyty w trybie fair, więc nie zakładamy kolejności,
w której oczekujące wątki dostaną permit. Test weryfikuje limit
współbieżności, a nie kolejność zadań.

Kolejność nie jest potrzebna do kontraktu „maksymalnie dwa wywołania
naraz”.

## Pytania kontrolne

1. Co dokładnie blokuje `releaseFirstPair.await(...)`?
2. Dlaczego test zwalnia zadania dopiero po sprawdzeniu licznika?
3. Dlaczego `AtomicInteger` jest użyty zamiast zwykłego `int`?
4. Po co zmniejszamy `activeCalls` w `finally`?
5. Czy test gwarantuje, które zadania otrzymają pierwsze dwa permits?
6. Czy limit semaphore dotyczy wszystkich instancji limitera, czy tylko
   jednej?

Do zapamiętania:

```text
Semaphore → limituje aktywne operacje
CountDownLatch → pozwala testowi sterować punktami synchronizacji
AtomicInteger → bezpieczny licznik współdzielony między zadaniami
```
