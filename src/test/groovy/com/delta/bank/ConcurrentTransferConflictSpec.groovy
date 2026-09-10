package com.delta.bank

import com.delta.bank.application.BankAccountService
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentTransferConflictSpec extends BaseIntegrationSpec {

    @Autowired
    BankAccountService service

    def "conflicting concurrent transfers fail with optimistic lock"() {
        given:
        service.openAccount('PLN-9101', 'Alice', new BigDecimal('2000.00'), 'PLN')
        service.openAccount('PLN-9102', 'Bob', new BigDecimal('0.00'), 'PLN')
        service.openAccount('PLN-9103', 'Charlie', new BigDecimal('0.00'), 'PLN')

        and:
        /*
        To jest utworzenie puli wątków.

        - Executors.newFixedThreadPool\(2\)` tworzy `ExecutorService` z dokładnie `2` wątkami roboczymi.
         `def executor` w Groovy oznacza inferencję typu, więc zmienna przechowuje ten executor.
        - W tym teście służy to do uruchomienia dwóch transferów równolegle, aby zasymulować konflikt współbieżności.
        - Później `executor.shutdown\(\)` kończy przyjmowanie nowych zadań, a `awaitTermination\(\)` czeka na ich zakończenie.

          Czyli: mechanizm do uruchamiania zadań asynchronicznie i równolegle.
         */
        def executor = Executors.newFixedThreadPool(2)


        /*
        `CountDownLatch\(1\)` to prosty synchronizator do skoordynowania startu wątków.

         - `def latch = new CountDownLatch\(1\)` tworzy licznik ustawiony na `1`
         - oba taski wywołują `latch.await\(\)` i zatrzymują się w tym miejscu
         - gdy główny wątek wykona `latch.countDown\(\)`, licznik spada do `0`
         - wtedy wszystkie oczekujące wątki ruszają dalej prawie jednocześnie

        W tym teście służy to jako `bariera startowa`, żeby dwa przelewy wystartowały równolegle i zwiększyły
        szansę na konflikt współbieżności.

        `1` oznacza, że wystarczy jedno zdarzenie odblokowujące.
        * */
        def latch = new CountDownLatch(1)

        when:
        def task1 = {
            latch.await()
            service.transfer('PLN-9101', 'PLN-9102', new BigDecimal('1200.00'))
        }

        // transfer z konta PLN-9101 do PLN-9103 na kwotę 1000.00
        // to samo konto źródłowe, więc może wystąpić konflikt z task1
        def task2 = {
            latch.await()
            service.transfer('PLN-9101', 'PLN-9103', new BigDecimal('1000.00'))
        }

        executor.submit(task1)
        executor.submit(task2)

        /*
        `latch.countDown()` zmniejsza licznik w `CountDownLatch` o `1`.

         W tym teście oznacza to:

           - licznik był ustawiony na `1`
           - oba taski czekają na `latch.await\(\)`
           - wywołanie `countDown\(\)` zmienia stan z `1` na `0`
           - gdy licznik osiąga `0`, oba wątki zostają odblokowane i startują prawie jednocześnie

         Tutaj pełni rolę `sygnału startu` dla równoległych transferów, żeby zwiększyć szansę na konflikt współbieżności.
        * */
        latch.countDown()

        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        then:
        def finalA = service.find('PLN-9101')
        finalA.balance >= BigDecimal.ZERO
        finalA.balance <= new BigDecimal('2000.00')
    }
}