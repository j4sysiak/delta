package com.delta.bank

import com.delta.bank.application.BankAccountService
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentTransferPessimisticSpec extends BaseIntegrationSpec {


    /*
    test sprawdza, że nie ma utraty pieniędzy
      1. i że dokładnie jedna operacja nie przechodzi,
         a nie konkretny numer salda, bo kolejność jest niedeterministyczna

         To znaczy, że test nie sprawdza dokładnego końcowego salda każdego konta osobno**,
         bo przy współbieżności nie da się przewidzieć, który przelew wykona się pierwszy.

         W tym przypadku:

        - są 2 równoległe przelewy: `800.00` i `500.00`
        - konto źródłowe ma tylko `1000.00`
        - więc **jeden przelew się powiedzie**, a **drugi powinien zostać odrzucony**
        - ale ponieważ kolejność wykonania wątków jest **niedeterministyczna**, nie zakłada się z góry, który z nich przegra

        Dlatego test sprawdza tylko, że:

            - **suma pieniędzy się zgadza** \(`source + target == 1000\.00`\)
            - **dokładnie jedna operacja zakończyła się błędem** \(`failures.size\(\) == 1`\)

        Czyli: test weryfikuje **własność biznesową**, a nie konkretny scenariusz kolejności wykonania.

     Dodatkowo:
      1. @Lock(PESSIMISTIC_WRITE) daje serializację
      2. nie wywala błędu „konfliktu wersji”
      3. tylko kolejno wykonuje transakcje i pozwala wyniki zależne od sekwencji
    **/
    @Autowired
    BankAccountService service

    def "pessimistic lock serializes conflicting transfers"() {
        given:
        service.openAccount('PLN-5001', 'Alice', new BigDecimal('1000.00'), 'PLN')
        service.openAccount('PLN-5002', 'Bob', new BigDecimal('0.00'), 'PLN')

        and:
        // Tworzy pulę 2 wątków do równoległego uruchomienia obu przelewów.
        def executor = Executors.newFixedThreadPool(2)
        // Blokuje start zadań, aby oba wątki ruszyły możliwie w tym samym momencie.
        def start = new CountDownLatch(1)
        // Przechowuje błędy z wątków w sposób bezpieczny dla współbieżności.
        def failures = Collections.synchronizedList([])

        when:
        executor.submit({
            // Czeka na wspólny sygnał startu, a następnie wykonuje przelew 800 PLN z konta Alice na konto Bob.
            // Jeśli operacja zakończy się błędem \- np. z powodu braku środków po wykonaniu drugiego przelewu \-
            // komunikat wyjątku zostanie zapisany na współdzielonej liście błędów.
            start.await()
            try {
                service.transfer('PLN-5001', 'PLN-5002', new BigDecimal('800.00'))
            } catch (Exception e) {
                failures << e.message
            }
        } as Runnable)

        executor.submit({
            // Czeka na wspólny sygnał startu, a następnie próbuje wykonać drugi przelew 500 PLN
            // z konta Alice na konto Bob. Jeśli po wcześniejszym przelewie zabraknie środków,
            // komunikat wyjątku zostanie zapisany na współdzielonej liście błędów.
            start.await()
            try {
                service.transfer('PLN-5001', 'PLN-5002', new BigDecimal('500.00'))
            } catch (Exception e) {
                failures << e.message
            }
        } as Runnable)

        // Zwolnienie blokady startowej uruchamia oba zadania możliwie jednocześnie.
        start.countDown()
        // Zamyka pulę na nowe zadania i czeka na zakończenie już uruchomionych przelewów.
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)

        then:
        def source = service.find('PLN-5001').balance
        def target = service.find('PLN-5002').balance

        source + target == new BigDecimal('1000.00')
        failures.size() == 1
    }
}