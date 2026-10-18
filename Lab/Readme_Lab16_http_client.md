# Lab 16 — `HttpClient` z Java 11

## Cel laba

W tym labie zbudujemy prostego klienta HTTP dla mini-banku. Przećwiczysz:

- tworzenie i ponowne używanie `HttpClient`,
- budowanie żądania `HttpRequest`,
- wysyłanie synchronicznego żądania przez `send(...)`,
- wybór `BodyHandler` i odczyt odpowiedzi,
- obsługę statusu HTTP,
- obsługę `IOException` i `InterruptedException`,
- testowanie klienta Spockiem bez prawdziwej sieci.

`java.net.http.HttpClient` jest dostępny od Java 11 i jest finalnym API.
Nie wymaga preview. Uruchamiamy kod na Java 21, nie zmieniamy konfiguracji
Gradle ani produkcyjnych klas mini-banku.

## 1. Najprostszy przepływ

```text
HttpClient → wysyła HttpRequest → otrzymuje HttpResponse<String>
```

Żądanie:

```java
var request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(3))
        .header("Accept", "text/plain")
        .GET()
        .build();
```

Wysłanie synchroniczne:

```java
var response = httpClient.send(
        request,
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
);
```

`send(...)` czeka na odpowiedź. Metoda może zgłosić:

- `IOException`, np. błąd połączenia lub odczytu,
- `InterruptedException`, jeśli wątek oczekujący został przerwany.

Metoda `send(...)` nie uznaje odpowiedzi HTTP 4xx lub 5xx za wyjątek
transportowy. Klient musi sam sprawdzić kod odpowiedzi.

## 2. `HttpClient` i `HttpRequest` to różne obiekty

- `HttpClient` jest klientem transportowym. Zwykle tworzysz go raz
  i używasz ponownie do wielu żądań.
- `HttpRequest` opisuje pojedyncze żądanie: URI, metodę, nagłówki,
  timeout i ewentualne body.
- `HttpResponse<T>` zawiera status, nagłówki, URI i body typu `T`.
- `BodyHandler<T>` mówi, jak odczytać body odpowiedzi.

`HttpClient` jest niemutowalny po zbudowaniu i przeznaczony do ponownego
użycia. Nie twórz nowego klienta dla każdego żądania.

## 3. Wyjątki a status odpowiedzi

To dwie różne kategorie problemów:

```text
IOException             → żądanie nie udało się na poziomie transportu
InterruptedException    → przerwano oczekiwanie
HTTP 404 / 503           → serwer odpowiedział, trzeba obsłużyć status
```

Po otrzymaniu odpowiedzi z kodem innym niż 2xx nasz klient zgłosi
własny `BankApiException`. Nie umieszczamy body odpowiedzi w wyjątku:
może zawierać dane, których nie należy logować.

## 4. Wyjątek do wklejenia

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\http\BankApiException.java
```

Wklej:

```java
package com.delta.bank.lab.java21.http;

public class BankApiException extends RuntimeException {

    private final int statusCode;

    public BankApiException(int statusCode) {
        super("Bank API returned HTTP status: " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
```

## 5. Klient do wklejenia

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\http\BankStatementHttpClient.java
```

Wklej:

```java
package com.delta.bank.lab.java21.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class BankStatementHttpClient {

    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public BankStatementHttpClient(
            HttpClient httpClient,
            Duration requestTimeout
    ) {
        this.httpClient = Objects.requireNonNull(
                httpClient,
                "httpClient must not be null"
        );
        this.requestTimeout = Objects.requireNonNull(
                requestTimeout,
                "requestTimeout must not be null"
        );

        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    public String getStatement(URI endpoint)
            throws IOException, InterruptedException {
        validateEndpoint(endpoint);

        var request = HttpRequest.newBuilder(endpoint)
                .timeout(requestTimeout)
                .header("Accept", "text/plain")
                .GET()
                .build();

        var response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BankApiException(response.statusCode());
        }

        return response.body();
    }

    private void validateEndpoint(URI endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");

        var scheme = endpoint.getScheme();
        if (!endpoint.isAbsolute()
                || !("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || endpoint.getFragment() != null) {
            throw new IllegalArgumentException(
                    "endpoint must be an absolute HTTP(S) URI without a fragment"
            );
        }
    }
}
```

Ten przykład przyjmuje pełne URI endpointu. W realnej aplikacji bazowy
adres serwisu i sposób budowania ścieżki byłyby zwykle osobną konfiguracją.

## 6. Spec Spocka do wklejenia

Test używa mocka `HttpClient`, więc nie wykonuje prawdziwego połączenia.
Sprawdzamy żądanie przekazane do transportu oraz odpowiedź zwróconą przez
mocka.

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\http\BankStatementHttpClientSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.http

import spock.lang.Specification

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class BankStatementHttpClientSpec extends Specification {

    def "sends GET request and returns successful response body"() {
        given:
        def endpoint = URI.create(
                "https://bank.example.test/api/accounts/PLN-1001/statement"
        )
        def response = Stub(HttpResponse) {
            statusCode() >> 200
            body() >> "Account statement for PLN-1001"
        }
        def httpClient = Mock(HttpClient)
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(3)
        )

        when:
        def body = client.getStatement(endpoint)

        then:
        1 * httpClient.send(
                {
                    HttpRequest request ->
                        request.uri() == endpoint &&
                                request.method() == "GET" &&
                                request.timeout().orElseThrow() ==
                                        Duration.ofSeconds(3) &&
                                request.headers()
                                        .firstValue("Accept")
                                        .orElse("") == "text/plain"
                },
                _ as HttpResponse.BodyHandler
        ) >> response

        and:
        body == "Account statement for PLN-1001"
    }

    def "throws BankApiException for a non-success HTTP status"() {
        given:
        def response = Stub(HttpResponse) {
            statusCode() >> 503
            body() >> "Do not include this body in the exception"
        }
        def httpClient = Stub(HttpClient) {
            send(
                    _ as HttpRequest,
                    _ as HttpResponse.BodyHandler
            ) >> response
        }
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
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
        def httpClient = Mock(HttpClient) {
            1 * send(
                    _ as HttpRequest,
                    _ as HttpResponse.BodyHandler
            ) >> { throw new InterruptedException("request interrupted") }
        }
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        client.getStatement(
                URI.create("https://bank.example.test/statement")
        )

        then:
        def exception = thrown(InterruptedException)
        exception.message == "request interrupted"
    }

    def "rejects a relative endpoint before sending a request"() {
        given:
        def httpClient = Mock(HttpClient)
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        client.getStatement(URI.create("/api/statement"))

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message ==
                "endpoint must be an absolute HTTP(S) URI without a fragment"
        0 * httpClient.send(_, _)
    }

    def "rejects an unsupported URI scheme before sending a request"() {
        given:
        def httpClient = Mock(HttpClient)
        def client = new BankStatementHttpClient(
                httpClient,
                Duration.ofSeconds(2)
        )

        when:
        client.getStatement(URI.create("file:///tmp/statement.txt"))

        then:
        thrown(IllegalArgumentException)
        0 * httpClient.send(_, _)
    }

    def "rejects a URI fragment before sending a request"() {
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
```

### Jeśli Spock zgłosi problem z przeciążeniem `send(...)`

`HttpClient.send` jest metodą generyczną. Jeśli dynamiczne dopasowanie
mocka w Twojej wersji Groovy/Spock sprawia problem, nie zmieniaj kodu
produkcyjnego ani zależności. Wklej treść błędu — dopasujemy spec do
wersji Spocka używanej w projekcie.

## 7. Uruchomienie

Git Bash, z katalogu projektu:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.http.BankStatementHttpClientSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.http.BankStatementHttpClientSpec"
```

Test jest odizolowany: nie wymaga działającego serwera, dostępu do
Internetu, Springa ani bazy danych.

## 8. Tworzenie prawdziwego klienta

Kod usługi dostaje `HttpClient` przez konstruktor. W miejscu składania
aplikacji można utworzyć i współdzielić klienta:

```java
var httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

var statementClient = new BankStatementHttpClient(
        httpClient,
        Duration.ofSeconds(3)
);
```

Potem można wywołać:

```java
var statement = statementClient.getStatement(
        URI.create("https://bank.example.test/api/accounts/PLN-1001/statement")
);
```

Kod wywołujący musi obsłużyć `IOException` i `InterruptedException`.
Jeśli przechwytuje `InterruptedException`, powinien zachować sygnał
przerwania:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("HTTP request was interrupted", exception);
}
```

Metoda klienta z laba propaguje `InterruptedException`, zamiast
przechwytywać ją i po cichu kontynuować. Odpowiedzialność za politykę
obsługi przerwania zostaje u wywołującego.

## 9. Timeouty i przekierowania

W przykładzie są dwa możliwe timeouty:

- `connectTimeout` na kliencie — limit czasu na nawiązanie połączenia,
- `HttpRequest.timeout(...)` — limit czasu na wykonanie danego żądania.

To odrębne ustawienia. Timeout połączenia nie jest pełnym limitem czasu
odpowiedzi.

Przekierowania są domyślnie obsługiwane polityką `NEVER`. W przykładzie
tworzenia klienta jawnie ustawiliśmy `Redirect.NORMAL`. Wybór polityki
zależy od kontraktu i bezpieczeństwa konkretnego serwisu.

## 10. Eksperymenty

### Eksperyment A: status 404

Zmień w teście status odpowiedzi z `503` na `404`. Klient powinien rzucić
`BankApiException` z kodem `404`. Sprawdza status, a nie sam fakt, czy kod
jest błędem klienta czy serwera.

### Eksperyment B: brak statusu 2xx

W kliencie tymczasowo zmień warunek:

```java
if (response.statusCode() != 200) {
```

Zobacz, jak zmienia to kontrakt: prawidłowe odpowiedzi takie jak `201`
lub `204` również zostaną odrzucone. Przywróć obsługę całego zakresu 2xx.

### Eksperyment C: metoda asynchroniczna

Java udostępnia też `sendAsync(...)`, który zwraca `CompletableFuture`.
Nie przerabiaj klienta w tym labie — najpierw opanuj prostą wersję
synchroniczną i jej obsługę błędów.

## Pytania kontrolne

1. Czym różnią się `HttpClient`, `HttpRequest` i `HttpResponse`?
2. Czy status HTTP 503 sam z siebie powoduje `IOException`?
3. Po co przekazujemy `BodyHandler` do `send(...)`?
4. Czym różni się timeout połączenia od timeoutu żądania?
5. Dlaczego warto ponownie używać instancji `HttpClient`?
6. Dlaczego kod nie powinien połykać `InterruptedException`?
7. Dlaczego testy używają mocka zamiast prawdziwego publicznego API?
8. Kiedy klient HTTP powinien zgłosić błąd domenowy dla statusu?

Do zapamiętania:

```text
HttpClient     → współdzielony transport
HttpRequest    → opis pojedynczego żądania
HttpResponse   → status, nagłówki i body
send(...)      → synchroniczne oczekiwanie
status != 2xx  → sprawdź i obsłuż jawnie
przerwanie     → propaguj albo przywróć flagę interrupt
```
