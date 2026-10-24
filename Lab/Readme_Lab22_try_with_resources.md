# Lab 22 — `try-with-resources`: kto zamyka zasób?

## Cel

Odkładamy współbieżność. W tym labie wykonuje się tylko jeden wątek,
a przykład ma pokazać jedną zasadę:

> Po wyjściu z `try-with-resources` Java automatycznie wywołuje `close()`.

Dotyczy to zarówno poprawnego zakończenia bloku, jak i wyjątku.

`try-with-resources` istnieje od Java 7. Od Java 9 można użyć w jego
nagłówku wcześniej zadeklarowanej zmiennej, jeśli jest `final` lub
effectively final. Przećwiczymy ten zapis na Java 21.
To przypomnienie zarządzania zasobami, nie nowa funkcja Java 21.

Bez Springa, bazy, sieci, wątków i preview. Nie zmieniaj konfiguracji
Gradle ani istniejących klas mini-banku.

## 1. Co jest zasobem?

Przykłady to otwarty plik, połączenie do bazy lub strumień odczytu.
Po zakończeniu używania trzeba je zamknąć.

Garbage collector nie zastępuje terminowego zamykania takich zasobów.

Na początek nie będziemy otwierać prawdziwego pliku. Utworzymy mały
obiekt szkoleniowy, żeby łatwo zobaczyć, kiedy następuje zamknięcie.

## 2. `AutoCloseable`

Interfejs `AutoCloseable` mówi:

```text
ten obiekt ma metodę close() i może być użyty w try-with-resources
```

Minimalny schemat:

```java
try (var resource = new SomeResource()) {
    resource.use();
}
```

Po zakończeniu bloku Java wywoła `resource.close()`.
Nie dopisujemy tego wywołania ręcznie.

## 3. Klasa do wklejenia

Utwórz plik:

```text
src\main\java\com\delta\bank\lab\java21\resources\StatementSession.java
```

Wklej:

```java
package com.delta.bank.lab.java21.resources;

public class StatementSession implements AutoCloseable {

    private boolean closed;

    public String readStatement() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }

        return "Statement for PLN-1001";
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
```

To symulacja zasobu, nie prawdziwa sesja bankowa ani połączenie.

Czytaj klasę tak:

```text
nowy obiekt    → closed = false
readStatement → zwraca tekst, jeśli sesja jest otwarta
close         → ustawia closed = true
```

Nie zajmujemy się tu bezpieczeństwem wątkowym: obiekt jest używany
w jednym wątku.

## 4. Java 9+: zmienna zadeklarowana przed `try`

```java
var session = new StatementSession();

try (session) {
    System.out.println(session.readStatement());
}

System.out.println(session.isClosed()); // true
```

Ten zapis pozwala nam zajrzeć do `session` po zamknięciu bloku.
Referencja nadal istnieje, ale zasób jest już zamknięty.

Zmienna `session` jest effectively final: przypisujemy ją raz i nie
przypisujemy jej później innego obiektu.

To nie znaczy, że sam obiekt jest niemutowalny. Jego pole `closed`
zmienia się podczas `close()`.

## 5. Mała klasa pokazująca oba scenariusze

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\resources\StatementReader.java
```

Wklej:

```java
package com.delta.bank.lab.java21.resources;

import java.util.Objects;

public class StatementReader {

    public String readAndClose(StatementSession session) {
        Objects.requireNonNull(session, "session must not be null");

        try (session) {
            return session.readStatement();
        }
    }

    public void failAndClose(StatementSession session) {
        Objects.requireNonNull(session, "session must not be null");

        try (session) {
            throw new IllegalStateException("Statement processing failed");
        }
    }
}
```

Nazwy metod celowo mówią, że przejmują odpowiedzialność za zamknięcie
przekazanej sesji. W rzeczywistym API trzeba jasno określić, kto jest
właścicielem zasobu i kto go zamyka.

### Poprawne wykonanie

```text
wejdź do try
odczytaj tekst
zamknij sesję
zwróć tekst do wywołującego
```

`return` wewnątrz bloku nie omija zamknięcia zasobu.

### Wyjątek

```text
wejdź do try
zgłoś wyjątek
zamknij sesję
przekaż wyjątek do wywołującego
```

Wyjątek nie omija zamknięcia zasobu.

## 6. Testy Spocka do wklejenia

Utwórz:

```text
src\test\groovy\com\delta\bank\lab\java21\resources\StatementReaderSpec.groovy
```

Wklej:

```groovy
package com.delta.bank.lab.java21.resources

import spock.lang.Specification

class StatementReaderSpec extends Specification {

    def "new session is open"() {
        given:
        def session = new StatementSession()

        expect:
        !session.isClosed()

        cleanup:
        session.close()
    }

    def "closes session before returning the statement"() {
        given:
        def session = new StatementSession()
        def reader = new StatementReader()

        when:
        def statement = reader.readAndClose(session)

        then:
        statement == "Statement for PLN-1001"
        session.isClosed()

        cleanup:
        session.close()
    }

    def "closes session even when processing fails"() {
        given:
        def session = new StatementSession()
        def reader = new StatementReader()

        when:
        reader.failAndClose(session)

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Statement processing failed"
        session.isClosed()

        cleanup:
        session.close()
    }

    def "cannot read a closed session"() {
        given:
        def session = new StatementSession()
        session.close()

        when:
        session.readStatement()

        then:
        def exception = thrown(IllegalStateException)
        exception.message == "Session is closed"
    }
}
```

`cleanup:` zabezpiecza porządkowanie po teście, nawet jeśli asercja nie
przejdzie. Nie maskuje braku zamknięcia w metodzie: `session.isClosed()`
jest sprawdzane wcześniej, w `then:`.

W naszej klasie ponowne `close()` jest bezpieczne — jeszcze raz ustawia
`closed` na `true`.

## 7. Uruchomienie

Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.resources.StatementReaderSpec"
```

PowerShell:

```powershell
.\gradlew.bat test --tests "com.delta.bank.lab.java21.resources.StatementReaderSpec"
```

Nie potrzebujesz bazy ani uruchomionej aplikacji.

## 8. Jeden eksperyment

Na chwilę zmień `readAndClose(...)` na:

```java
public String readAndClose(StatementSession session) {
    Objects.requireNonNull(session, "session must not be null");
    return session.readStatement();
}
```

Uruchom testy. Odczyt tekstu nadal działa, ale test sprawdzający zamknięcie
sesji nie przejdzie.

Dlaczego? Usunąłeś `try-with-resources`, więc nic nie wywołało `close()`
przed asercją.

Przywróć poprzednią implementację.

## 9. Co jeśli `close()` też rzuci wyjątek?

Na razie wystarczy znać regułę, bez dodatkowego kodu:

- jeśli blok zakończy się poprawnie, wyjątek z `close()` zostanie zgłoszony,
- jeśli blok i `close()` zgłoszą wyjątki, wyjątek z bloku pozostanie
  główny, a wyjątek zamykania trafi do `getSuppressed()`.

Nie oznacza to, że błąd zamknięcia jest ignorowany.

## Pytania kontrolne

1. Kto wywołuje `close()` w `try-with-resources`?
2. Czy `return` w bloku pomija zamknięcie zasobu?
3. Czy wyjątek w bloku pomija zamknięcie zasobu?
4. Co oznacza effectively final dla zmiennej `session`?
5. Czy GC zastępuje jawne zamykanie plików i połączeń?

Do zapamiętania:

```text
AutoCloseable → obiekt ma metodę close()
try-with-resources → automatycznie wywołuje close() przy wyjściu z bloku
return i wyjątek → również powodują zamknięcie
```
