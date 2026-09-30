# Lab 7 — Java: `Clock` i deterministyczny czas w testach Spocka

## Cel laba

W tym labie uczymy się przekazywać źródło czasu do kodu zamiast wywoływać
bezpośrednio:

```java
LocalDateTime.now()
```

Zależność od systemowego zegara utrudnia testowanie. Wynik może zależeć od
chwili uruchomienia testu, strefy czasowej komputera lub środowiska CI.

Zamiast tego wstrzykniemy `java.time.Clock` i w testach użyjemy zegara
ustawionego na konkretny moment.

Ten lab będzie osobnym ćwiczeniem. Nie zmieniamy istniejącej konfiguracji
Spring Data Auditing ani kodu mini-banku.

## Pliki laba

### Kod Java

```text
src/main/java/com/delta/bank/lab/java21/clock/BankOperationAudit.java
src/main/java/com/delta/bank/lab/java21/clock/ClockBasedAuditService.java
```

### Test Spock

```text
src/test/groovy/com/delta/bank/lab/java21/clock/ClockBasedAuditServiceSpec.groovy
```

## 1. Problem z bezpośrednim odczytem czasu

Przykład trudny do deterministycznego testowania:

```java
public LocalDateTime recordTime() {
    return LocalDateTime.now();
}
```

Test nie może kontrolować, co zwróci `now()`. Musiałby porównywać wynik
z czasem odczytanym obok, co wprowadza ryzyko różnicy oraz zależność od
zegarów systemowych.

## 2. Wstrzyknięcie `Clock`

`Clock` reprezentuje źródło czasu i strefę czasową:

```java
public class ClockBasedAuditService {

    private final Clock clock;

    public ClockBasedAuditService(Clock clock) {
        this.clock = clock;
    }
}
```

Kod produkcyjny może korzystać z czasu przez:

```java
LocalDateTime.now(clock)
```

W teście możemy przekazać `Clock.fixed(...)` i ustalić konkretny moment.

## 3. Stały zegar w teście

```groovy
def fixedInstant = Instant.parse("2026-09-19T12:30:00Z")
def clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
```

Każde odczytanie czasu z tego zegara zwróci ten sam moment.

Przykładowa asercja:

```groovy
result.occurredAt() == LocalDateTime.of(2026, 9, 19, 12, 30)
```

Test jest deterministyczny: nie zależy od aktualnej daty ani godziny.

## 4. `Instant`, `Clock` i strefa czasowa

- `Instant` reprezentuje konkretny moment na osi czasu, niezależny od strefy.
- `Clock` dostarcza moment i strefę czasową.
- `LocalDateTime` jest lokalną datą i godziną, bez informacji o strefie.

Przykład:

```java
LocalDateTime.now(clock)
```

wynik zależy od strefy ustawionej w `clock`.

W aplikacjach rozproszonych często warto przechowywać lub przekazywać czas
jako `Instant`, a lokalny czas tworzyć dopiero na granicy prezentacji.
W tym labie użyjemy `LocalDateTime`, ponieważ mini-bank używa go w polach
audytowych.

## 5. Testowanie różnych stref

Ten sam `Instant` może dać różne lokalne daty i godziny w zależności od strefy:

```groovy
def instant = Instant.parse("2026-09-19T12:30:00Z")

def utcClock = Clock.fixed(instant, ZoneOffset.UTC)
def warsawClock = Clock.fixed(
        instant,
        ZoneId.of("Europe/Warsaw")
)
```

Zegar warszawski i UTC wskazują ten sam moment. Różni się lokalna
reprezentacja czasu.

## 6. Zmiana czasu testowego przez `Clock.offset`

Można utworzyć zegar przesunięty względem innego:

```java
Clock laterClock = Clock.offset(
        baseClock,
        Duration.ofHours(1)
);
```

To bywa przydatne do testowania zachowań zależnych od upływu czasu bez
czekania w teście.

Nie należy używać `Thread.sleep(...)` do przesuwania czasu testowego.

## 7. Zakres ćwiczenia

Utworzymy:

- rekord `BankOperationAudit` z numerem konta, opisem operacji i czasem,
- `ClockBasedAuditService`, który przyjmuje `Clock` w konstruktorze,
- spec Spocka testujący dokładną wartość czasu dla zegara stałego.

Przykładowy przepływ:

```text
Clock.fixed(...)
        ↓
ClockBasedAuditService
        ↓
BankOperationAudit z dokładnym, przewidywalnym occurredAt
```

## 8. `Clock` jako zależność produkcyjna

W rzeczywistym kodzie można udostępnić zegar Spring beanem:

```java
@Bean
Clock clock() {
    return Clock.systemDefaultZone();
}
```

Serwis przyjmuje go przez konstruktor. Dzięki temu produkcja używa zegara
systemowego, a test może przekazać zegar stały.

W tym labie najpierw testujemy ten wzorzec poza kontekstem Springa, aby
skupić się na Javie i Spocku. Integrację z audytem JPA można zrobić jako
osobne ćwiczenie.

## 9. Typowe błędy

### Bezpośrednie `now()` w kodzie testowanym

```java
LocalDateTime.now()
```

utrudnia kontrolowanie czasu w teście.

### Używanie `Clock.systemDefaultZone()` w teście

Test zależy wtedy od zegara i strefy komputera, na którym jest uruchomiony.
W teście deterministycznym używaj `Clock.fixed(...)`.

### Mylenie czasu lokalnego z momentem

`LocalDateTime` nie zawiera strefy ani offsetu. Nie jest samodzielnym,
jednoznacznym momentem na osi czasu.

### Używanie `Thread.sleep(...)` do testowania upływu czasu

Test staje się wolniejszy i podatny na obciążenie środowiska. Przekazany
`Clock` pozwala przesuwać lub ustalać czas bez czekania.

## 10. Pytania rekrutacyjne

### Po co wstrzykiwać `Clock`?

Żeby odseparować źródło czasu od logiki biznesowej i móc deterministycznie
testować zachowanie zależne od czasu.

### Czym różni się `Clock.fixed` od `Clock.systemDefaultZone`?

`Clock.fixed` zawsze zwraca ustalony moment. `Clock.systemDefaultZone`
odczytuje aktualny czas systemowy w domyślnej strefie.

### Czym różnią się `Instant` i `LocalDateTime`?

`Instant` oznacza jednoznaczny moment UTC na osi czasu. `LocalDateTime`
to lokalna data i godzina bez strefy.

### Czy `Clock` przyspiesza testy?

Sam w sobie nie. Pozwala jednak testować upływ czasu bez czekania i bez
`Thread.sleep(...)`.

### Czy to nowa funkcja Java 21?

Nie. `Clock` jest dostępny od Java 8. Jest jednak nadal ważnym wzorcem
w nowoczesnej Javie i bardzo użytecznym elementem testowalnego projektu.

## 11. Uruchomienie testu

Po ręcznym dodaniu klas i speca uruchom:

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.lab.java21.clock.ClockBasedAuditServiceSpec"
```

### Git Bash

```bash
cd /c/dev/delta
./gradlew test --tests "com.delta.bank.lab.java21.clock.ClockBasedAuditServiceSpec"
```

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

Najważniejszy wzorzec laba:

```java
public ClockBasedAuditService(Clock clock) {
    this.clock = clock;
}
```

Kod korzysta z:

```java
LocalDateTime.now(clock)
```

Test przekazuje:

```groovy
Clock.fixed(instant, ZoneOffset.UTC)
```

Dzięki temu test dokładnie kontroluje czas i nie zależy od zegara systemowego.
