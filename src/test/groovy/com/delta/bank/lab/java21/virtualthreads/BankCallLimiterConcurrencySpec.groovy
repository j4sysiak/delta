package com.delta.bank.lab.java21.virtualthreads

import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BankCallLimiterConcurrencySpec extends Specification {

    def "nigdy nie dopuszcza do większej liczby równoczesnych wywołań niż skonfigurowano"() {
        given:
        int limit = 2
        int taskCount = 4
        def limiter = new BankCallLimiter(limit)

        // Licznik testowy aktywnych bloków przekazanych do limiter.execute(...).
        // Nie ogranicza współbieżności — służy wyłącznie do pomiaru.
        def activeCalls = new AtomicInteger()

        def maximumActiveCalls = new AtomicInteger()

        // Pierwszy Latch otworzy się, gdy dwa zadania wejdą do bloku limitera.
        def enteredFirstPair = new CountDownLatch(limit)

        // Drugi Latch pozwala zatrzymać te zadania, aż test sprawdzi liczniki.
        def releaseFirstPair = new CountDownLatch(1)

        def allCompleted = new CountDownLatch(taskCount)

        when:
        def results = []
        def executor = Executors.newVirtualThreadPerTaskExecutor()
        try {
            def futures = (1..taskCount).collect { taskNumber ->
                // Każde zadanie jest uruchamiane na osobnym wirtualnym wątku;
                // submit zwraca Future z jego wynikiem.
                executor.submit({
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
                                return "result-${taskNumber}"  // Zwraca wynik z numerem zadania
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

            assert enteredFirstPair.await(5, TimeUnit.SECONDS)
            assert activeCalls.get() == limit
            assert maximumActiveCalls.get() == limit

            // Po sprawdzeniu liczników pozwala zadaniom kontynuować.
            releaseFirstPair.countDown()

            results = futures.collect { it.get(5, TimeUnit.SECONDS) }
        } finally {
            // W razie błędu zwolnij zadania czekające na latch, a następnie zamknij executor.
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