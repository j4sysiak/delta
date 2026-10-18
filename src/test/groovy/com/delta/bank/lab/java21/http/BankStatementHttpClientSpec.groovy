package com.delta.bank.lab.java21.http

import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class BankStatementHttpClientSpec extends Specification {

    def "sends GET request and returns successful response body"() {
        given:
        // Testowy, absolutny adres endpointu wyciągu — używany do weryfikacji,
        // że klient buduje i wysyła żądanie GET pod oczekiwany URI.
        def endpoint = URI.create(
                "https://bank.example.test/api/accounts/PLN-1001/statement"
        )
        // Symulowana odpowiedź HTTP: status 200 i przykładowe body,
        // bez wykonywania rzeczywistego requestu do zewnętrznego API.
        def response = Stub(HttpResponse) {
            statusCode() >> 200
            body() >> "Account statement for PLN-1001"
        }
        // Mock zamiast Stub, ponieważ test sprawdza interakcję z HttpClient:
        // jedno wywołanie send(...) oraz poprawnie zbudowane żądanie HTTP.
        // czyli bedziemy naprawdę testować klasę produkcyjną BankStatementHttpClient, a nie tylko stubować jej zachowanie.
        // dlatego właśnie używamy Mock(HttpClient), a nie Stub(HttpClient).
        def httpClient = Mock(HttpClient)

        // Tworzy testowanego klienta z podstawionym mockiem HttpClient
        // oraz timeoutem 3 s, aby zweryfikować konfigurację żądania
        // bez wykonywania realnego połączenia HTTP.
        def client = new BankStatementHttpClient(
                httpClient,  // to jest mock, który będzie weryfikowany pod kątem wywołań send(...)
                Duration.ofSeconds(3)
        )

        when:
        def body = client.getStatement(endpoint) // wejście do produkcyjnej metody, która ma wysłać żądanie GET i zwrócić body odpowiedzi.

        then:
        // To jest weryfikacja interakcji z mockiem httpClient w Spocku.
        // Ten fragment mówi: podczas wykonania testu ma się zdarzyć dokładnie jedno wywołanie httpClient.send(...),
        // a pierwszy argument (HttpRequest) musi spełnić podane warunki.
        // 1.   1 * httpClient.send(...) — metoda send ma zostać wywołana raz.
        // 2.   { HttpRequest request -> ... } — to matcher argumentu: Spock sprawdza, czy przekazany HttpRequest spełnia warunki w bloku.
        //    - request.uri() == endpoint — ma uri() równe endpoint,
        //    - request.method() == "GET" — metoda żądania to GET,
        //    - request.timeout().orElseThrow() == Duration.ofSeconds(3) — timeout żądania ustawiony na 3 sekundy,
        //    - request.headers().firstValue("Accept").orElse("") == "text/plain" — nagłówek Accept ustawiony na "text/plain".
        // 3.   _ as HttpResponse.BodyHandler — drugi argument (BodyHandler) może być dowolnym obiektem implementującym interfejs BodyHandler

        // ten test nie tylko sprawdza wynik body, ale też to, że klient HTTP zbudował poprawne żądanie i wysłał je w oczekiwany sposób.
        1 * httpClient.send(
                {
                    // to jest Closure, który przyjmuje argument request typu HttpRequest
                    HttpRequest request ->  // po -> jest warunek zwracający true albo false
                                            // jeśli warunek jest true, to Spock uznaje, że pierwszy argument metody send(...) pasuje
                                            // jeśli false, interakcja nie zostaje spełniona i test poleci
                        // Cała linia::
                        // 1 * httpClient.send( { ... }, _ as HttpResponse.BodyHandler ) >> response
                        // 1 * — metoda ma być wywołana dokładnie raz
                        // { ... } — pierwszy argument ma spełnić te warunki
                        // _ as HttpResponse.BodyHandler — drugi argument może być dowolny, ale ma być tego typu
                        // >> response — wtedy mock ma zwrócić response
                        // Czyli to nie jest „zwykły blok kodu”, tylko closure użyte przez Spocka jako predykat sprawdzający argument metody.
                        request.uri() == endpoint &&   // ma uri() równe endpoint
                                request.method() == "GET" &&   // ma metodę "GET"
                                request.timeout().orElseThrow() == Duration.ofSeconds(3) &&   // timeout żądania ustawiony na 3 sekundy
                                request.headers()
                                        .firstValue("Accept")
                                        .orElse("") == "text/plain"  // ma nagłówek Accept: text/plain.
                },
                _ as HttpResponse.BodyHandler  // _ as HttpResponse.BodyHandler — drugi argument może być dowolny, byle był typu HttpResponse.BodyHandler.
        ) >> response  // — jeśli takie wywołanie nastąpi, mock ma zwrócić wcześniej przygotowany response.

        and:
        body == "Account statement for PLN-1001"
    }

    def "throws BankApiException for a non-success HTTP status"() {
        given:
        // Symulowana odpowiedź HTTP: status 503 i przykładowe body,
        // bez wykonywania rzeczywistego requestu do zewnętrznego API.
        def response = Stub(HttpResponse) {
            statusCode() >> 503
            body() >> "Do not include this body in the exception"
        }

        // To tworzy Stub klasy HttpClient i ustawia jego zachowanie dla metody send(...).
        // Innymi słowy: zamiast prawdziwego klienta HTTP test dostaje atrapę,
        // która dla dowolnego HttpRequest i dowolnego HttpResponse.BodyHandler zawsze zwróci wcześniej przygotowany response.
        def httpClient = Stub(HttpClient) {
            send(
                    _ as HttpRequest,  // dla dowolnego HttpRequest
                    _ as HttpResponse.BodyHandler  // dla dowolnego HttpResponse.BodyHandler
            ) >> response  // zawsze zwróci wcześniej przygotowany response
                           // W tym teście nie interesuje nas jak dokładnie send() zostało wywołane, tylko że odpowiedź ma status 503
                           // dlatego używamy Stub(HttpClient) zamiast Mock(HttpClient), bo nie weryfikujemy interakcji, tylko zachowanie.
                           // chcemy sprawdzić, czy BankStatementHttpClient rzuci BankApiException.
        }

        // Tworzy instancję testowanego klienta z podstawionym mockiem/stubem HttpClient
        // oraz timeoutem 2 sekundy, żeby sprawdzić zachowanie klasy bez realnego połączenia HTTP.
        // konfigurują obiekt produkcyjny do testu:
        // wstrzykują kontrolowanego httpClient i ustawiają timeout używany później w scenariuszu.
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        // To jest wywołanie testowanej metody getStatement(...) z poprawnym, absolutnym adresem HTTPS.
        // W tym teście te linie należą do sekcji when:,
        // więc uruchamiają właściwy scenariusz: BankStatementHttpClient ma spróbować wysłać żądanie pod wskazany endpoint.
        // W kontekście tego testu nie chodzi o sam URI, tylko o efekt uboczny tego wywołania
        // — wcześniej httpClient został skonfigurowany jako Mock, który przy send(...) rzuca InterruptedException("request interrupted").
        // Dzięki temu te 3 linie prowokują błąd, a sekcja then:
        // może sprawdzić, że wyjątek został przekazany dalej do wywołującego, a nie np. opakowany lub ukryty.
        client.getStatement(
                URI.create("https://bank.example.test/statement")
        )

        then:
        def exception = thrown(BankApiException)
        exception.statusCode() == 503
        exception.message == "Bank API returned HTTP status: 503"
        !exception.message.contains("Do not include this body")
    }

    def "propagates an interrupted request to its caller"() {
        given:
        // To tworzy Stub klasy HttpClient i ustawia jego zachowanie dla metody send(...).
        // Innymi słowy: zamiast prawdziwego klienta HTTP test dostaje atrapę,
        // która dla dowolnego HttpRequest i dowolnego HttpResponse.BodyHandler zawsze zwróci wcześniej przygotowany response.
        // oznacza to: dokładnie 1 raz ma zostać wywołane send(...), a to wywołanie ma zakończyć się wyjątkiem.
        def httpClient = Mock(HttpClient) {
            1 * send(
                    _ as HttpRequest,  // dla dowolnego HttpRequest
                    _ as HttpResponse.BodyHandler // dla dowolnego HttpResponse.BodyHandler
            ) >> { throw new InterruptedException("request interrupted") }
                 // >> — mówi, co mock ma zrobić po dopasowanym wywołaniu,
                 // { ... } — to closure wykonywane w momencie wywołania metody,
                 // To linia konfigurująca mocka w Spocku: gdy httpClient.send(...) zostanie wywołane z pasującymi argumentami,
                 // mock nie zwróci wyniku, tylko rzuci InterruptedException z komunikatem "request interrupted".
                 // throw new InterruptedException(...) — symuluje przerwane żądanie HTTP.
        }


        // tutaj powstaje testowana instancja BankStatementHttpClient z wstrzykniętym httpClientem, który jest mockiem.
        // Dzięki temu test może sprawdzić, czy metoda getStatement(...) poprawnie propaguje wyjątek InterruptedException do wywołującego.
        // Tworzy testowaną instancję klienta z podstawionym httpClient
        // i timeoutem 2 sekundy, aby przetestować logikę klasy
        // bez wykonywania realnego requestu HTTP.
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:

        // Wywołuje testowaną metodę z poprawnym adresem endpointu,
        // żeby uruchomić scenariusz wysłania żądania HTTP.
        // W tym teście celem nie jest wynik odpowiedzi, tylko sprawdzenie,
        // że przerwanie podczas send(...) zostanie przekazane dalej
        // jako InterruptedException.

        // Ten fragment w sekcji when:
        // inicjuje właściwe wykonanie testu — dopiero tutaj BankStatementHttpClient próbuje użyć podstawionego httpClient,
        // który w tym scenariuszu rzuca InterruptedException. Dzięki temu then: może zweryfikować propagację wyjątku.
        client.getStatement(
                URI.create("https://bank.example.test/statement")
        )

        then:
        def exception = thrown(InterruptedException)
        exception.message == "request interrupted"
    }

    // jeśli endpoint nie jest absolutnym adresem HTTP/HTTPS, klient ma odrzucić go od razu, zanim spróbuje cokolwiek wysłać.
    def "sprawdza walidację wejścia przed wykonaniem requestu HTTP"() {
        given:
        def httpClient = Mock(HttpClient)

        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        // wywołuje getStatement() z relatywnym URI: "/api/statement" zamiast pełnego http://... lub https://...,
        client.getStatement(URI.create("/api/statement"))

        then:
        // oczekuje, że metoda rzuci IllegalArgumentException z komunikatem
        //"endpoint must be an absolute HTTP(S) URI without a fragment",
        def exception = thrown(IllegalArgumentException)
        exception.message ==
                "endpoint must be an absolute HTTP(S) URI without a fragment"

        // i dodatkowo potwierdza, że żadne żądanie nie zostało wysłane: 0 * httpClient.send(_, _).
        0 * httpClient.send(_, _)
    }

    def "Ten test sprawdza walidację URI przed wysłaniem requestu."() {
        given:
        def httpClient = Mock(HttpClient)
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        // wywołuje getStatement(...) z adresem file:///tmp/statement.txt
        // oczekuje IllegalArgumentException
        // i potwierdza, że nie doszło do żadnego wywołania httpClient.send(_, _).
        client.getStatement(URI.create("file:///tmp/statement.txt"))

        then:
        thrown(IllegalArgumentException)

        // Potwierdza, że klient HTTP nie wysłał żadnego żądania — walidacja ma zakończyć metodę wcześniej.
        0 * httpClient.send(_, _)
    }

    def "sprawdza, że BankStatementHttpClient odrzuca URI z fragmentem (#section) zanim spróbuje wysłać request HTTP"() {
        given:
        def httpClient = Mock(HttpClient)
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        client.getStatement(
                URI.create("https://bank.example.test/statement#section")
        )

        then:
        thrown(IllegalArgumentException)
        0 * httpClient.send(_, _)
    }

    def "rejects a non-positive request timeout"() {
        when:
        new BankStatementHttpClient(
                Stub(HttpClient),
                Duration.ZERO
        )

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "requestTimeout must be positive"
    }

    def "rejects null endpoint"() {
        given:
        def client = new BankStatementHttpClient(
                Stub(HttpClient),
                Duration.ofSeconds(2)
        )

        when:
        client.getStatement(null)

        then:
        def exception = thrown(NullPointerException)
        exception.message == "endpoint must not be null"
    }
}