package com.delta.bank.lab.java21.virtualthreads

import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BankCallLimiterConcurrencySpec extends Specification {
//
//    Najprościej: test uruchamia **4 zadania**, ale limiter ma limit **2**. Sprawdza, czy naraz wejdą do bloku najwyżej dwa.
//
//    1. Każde zadanie próbuje wykonać `limiter.execute(...)`.
//       Dwa pierwsze, które uzyskają miejsce, zwiększają `activeCalls` i czekają na `releaseFirstPair`.
//    2. Każde z nich zmniejsza `enteredFirstPair`.
//       Gdy licznik spadnie do zera, `enteredFirstPair.await()` w teście kończy czekanie.
//       Wtedy test sprawdza, że aktywne są dokładnie dwa wywołania i że maksimum też wynosi dwa.
//       Pozostałe zadania w tym czasie czekają na limiter.
//    3. Test wywołuje `releaseFirstPair.countDown()`.
//       Dwa czekające zadania mogą dokończyć pracę, zwalniają miejsca w limiterze, a następne dwa zadania wchodzą do bloku.
//    4. `futures.collect { it.get(...) }` czeka na wyniki wszystkich zadań.
//        Na końcu test sprawdza, że wszystkie się zakończyły, licznik aktywnych wrócił do zera, maksimum nie przekroczyło limitu i wyniki są poprawne.
//
//    `activeCalls` tylko **mierzy** liczbę aktywnych bloków — sam jej nie ogranicza.
//     Ograniczenie zapewnia `limiter`.
//     `releaseFirstPair` jest jak bramka: test trzyma ją zamkniętą, żeby móc sprawdzić liczniki, a potem otwiera.
//

    def "nigdy nie dopuszcza do większej liczby równoczesnych wywołań niż skonfigurowano"() {
        given:
        int limit = 2
        int taskCount = 4
        def limiter = new BankCallLimiter(limit)

        // Licznik testowy aktywnych bloków przekazanych do limiter.execute(...).
        // Nie ogranicza współbieżności — służy wyłącznie do pomiaru.
        def activeCalls = new AtomicInteger()

        def maximumActiveCalls = new AtomicInteger()

        // Latch (zatrzask) to mechanizm synchronizacji:
        //   w CountDownLatch licznik zmniejsza się po każdym countDown(), a await() czeka, aż spadnie do zera.
        // Taki latch działa tylko raz — nie można go ponownie „zatrzasnąć”.
        // Tutaj enteredFirstPair startuje z licznikiem 2.
        // Gdy dwa zadania wejdą do bloku limitera, każde wywoła countDown(),
        // a test przestanie czekać w enteredFirstPair.await().
        // releaseFirstPair działa odwrotnie z punktu widzenia testu:
        //    zatrzymane zadania czekają na nim, aż test wywoła countDown().

        // Pierwszy Latch otworzy się, gdy dwa zadania wejdą do bloku limitera.
        def enteredFirstPair = new CountDownLatch(limit)

        // Drugi Latch pozwala zatrzymać te zadania, aż test sprawdzi liczniki.
        def releaseFirstPair = new CountDownLatch(1)

        def allCompleted = new CountDownLatch(taskCount)

        when:
        def results = []
        def executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
                                         // test uruchamia **4 zadania**
            def futures = (1..taskCount).collect { taskNumber ->
                // Każde zadanie jest uruchamiane na osobnym wirtualnym wątku;

                // submit zwraca Future z jego wynikiem.
                executor.submit({               // <T> Future<T> submit(Callable<T> task);
                    try {

                        // Limiter uruchamia blok dopiero po uzyskaniu wolnego miejsca w limicie.
                        return limiter.execute({
                            // zwiększa licznik aktywnych wywołań
                            int current = activeCalls.incrementAndGet()

                            // aktualizuje dotychczasowe maksimum
                            maximumActiveCalls.accumulateAndGet(
                                    current,
                                    Math::max
                            )

                            // Sygnalizuje, że zadanie weszło do bloku i zwiększyło licznik aktywnych wywołań.
                            enteredFirstPair.countDown()

                            try {
                                if (!releaseFirstPair.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException(
                                            "test did not release first tasks"
                                    )
                                }
                                return "result-${taskNumber}"  // i w końcu zwraca wynik z numerem zadania
                            } finally {
                                activeCalls.decrementAndGet()
                            }
                        } as Callable<String>)
                    } finally {
                        // Sygnalizuje zakończenie zadania.
                        allCompleted.countDown()
                    }
                } as Callable<String>)
            }

            // Ta linia czeka maksymalnie 5 sekund, aż `enteredFirstPair` osiągnie zero
            //    — czyli aż dwa zadania wejdą do bloku limitera i wywołają `countDown()`.
            // Jeśli to się nie stanie na czas, `await()` zwróci `false`, a asercja obleje test.
            assert enteredFirstPair.await(5, TimeUnit.SECONDS)
            // Ta asercja sprawdza, że w chwili jej wykonania aktywne są dokładnie `limit` wywołania — tutaj dwa.
            // Potwierdza, że pierwsza para weszła do bloku limitera, zanim test ją zwolni;
            // sama nie sprawdza, czy limit nigdy nie został przekroczony (od tego jest `maximumActiveCalls`).
            assert activeCalls.get() == limit


            // Ta asercja potwierdza, że maksimum zmierzonych równoczesnych wywołań wyniosło dokładnie `limit`
            //  — test sprawdził więc, że limiter dopuścił dwa wywołania, ale nie więcej.
            //  `maximumActiveCalls` jest aktualizowane przy każdym wejściu do bloku.
            assert maximumActiveCalls.get() == limit

            // Po sprawdzeniu liczników pozwala zadaniom kontynuować.
            releaseFirstPair.countDown()

            // Pobiera wyniki wszystkich zadań; na każde oczekiwanie na Future przypada maksymalnie 5 sekund.
            // Wyniki zachowują kolejność z listy `futures`, a nie kolejność zakończenia zadań.
            results = futures.collect { it.get(5, TimeUnit.SECONDS) }
        } finally {
            // W razie błędu zwolnij zadania czekające na Latch, a następnie zamknij executor.
            releaseFirstPair.countDown()
            executor.close()
        }

        then:
        allCompleted.count == 0
        maximumActiveCalls.get() == limit
        activeCalls.get() == 0

        // Ta asercja sprawdza, czy wszystkie cztery zadania zwróciły oczekiwane wyniki.
        // toSet() ignoruje kolejność, więc nie ma znaczenia, w jakiej kolejności zadania się zakończyły.
        // Jeśli któregoś wyniku brakuje albo pojawił się nieoczekiwany, porównanie zbiorów nie przejdzie.
        results.toSet() == ["result-1", "result-2", "result-3", "result-4"].toSet()
    }
}