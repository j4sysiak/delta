# Lab 10 — Java 21: Scoped Values

## Cel laba

W tym labie poznajemy `ScopedValue` w wersji preview dostępnej w JDK 21.
Użyjemy go do przekazywania niezmiennego kontekstu żądania bankowego do
powiązanych zadań:

```text
żądanie bankowe
├── correlationId
├── użytkownik
└── zadania podrzędne
    ├── odczyt salda
    ├── odczyt historii
    └── odczyt podsumowania
```

Nauczymy się:

- tworzyć klucz `ScopedValue`,
- wiązać wartość z ograniczonym zakresem,
- odczytywać kontekst bez przekazywania go przez każdy argument metody,
- rozumieć dziedziczenie wartości przez zadania w Structured Concurrency,
- testować widoczność i izolację kontekstu w Spocku.

Lab 10 jest kontynuacją Lab 3 i Lab 9. Virtual Threads wykonują zadania,
Structured Concurrency organizuje ich cykl życia, a Scoped Values przekazują
im niezmienny kontekst.

## Ważne: API w JDK 21 jest preview

`ScopedValue` było API preview w JDK 21. Do kompilacji i uruchamiania trzeba
użyć `--enable-preview` oraz JDK 21.

Jeżeli konfiguracja z Lab 9 jest już w `build.gradle`, nie dodawaj jej
ponownie. Sprawdź tylko, czy kompilacja i JVM uruchamiająca testy mają
włączone preview. API preview może różnić się pomiędzy wersjami JDK, dlatego
przykłady z tego laba odnoszą się konkretnie do JDK 21.

## 1. Problem: przekazywanie kontekstu przez argumenty

Bez Scoped Values kontekst trzeba przekazywać przez kolejne metody:

```java
loadBankData(accountNumber, requestContext);
loadBalance(accountNumber, requestContext);
loadHistory(accountNumber, requestContext);
loadSummary(accountNumber, requestContext);
```

W większym łańcuchu wywołań argument kontekstu może przechodzić przez wiele
metod, które same go nie wykorzystują. Scoped Value pozwala odczytać
niezmienny kontekst w obrębie jawnie wyznaczonego zakresu.

## 2. Czym jest `ScopedValue`

`ScopedValue<T>` jest kluczem do wartości dostępnej tylko w określonym
zakresie wykonania. W uproszczeniu:

```java
private static final ScopedValue<RequestContext> REQUEST_CONTEXT =
        ScopedValue.newInstance();
```

Wartość wiąże się z zakresem operacji:

```java
ScopedValue.where(REQUEST_CONTEXT, context).run(() -> {
    // Kod w tym zakresie może odczytać REQUEST_CONTEXT.
});
```

Wewnątrz zakresu wartość można odczytać przez:

```java
RequestContext context = REQUEST_CONTEXT.get();
```

Po zakończeniu zakresu wiązanie przestaje być dostępne. Nie jest to globalna
zmienna ani magazyn wartości modyfikowanej przez zadania.

## 3. Powiązanie ze Structured Concurrency

W tym labie kontekst zostanie związany przed uruchomieniem zadań
Structured Concurrency. Powiązane zadania podrzędne mogą odczytać tę samą
wartość, dzięki czemu nie trzeba przekazywać kontekstu w każdym wywołaniu
`fork(...)`.

Schemat operacji:

```text
zwiąż RequestContext
    uruchom StructuredTaskScope
        fork: odczyt salda
        fork: odczyt historii
        fork: odczyt podsumowania
    scope czeka na zadania i kończy pracę
zakończ zakres ScopedValue
```

Wiązanie musi obejmować kod, który tworzy i uruchamia zadania podrzędne.
Nie należy zakładać, że niezależne zadanie uruchomione poza tym zakresem
automatycznie otrzyma kontekst.

## 4. Zakres i izolacja

Wartość Scoped Value:

- jest dostępna tylko podczas aktywnego wiązania,
- jest przeznaczona do odczytu, a nie do współdzielonej modyfikacji,
- może być przesłonięta przez nowe wiązanie w zagnieżdżonym zakresie,
- po wyjściu z zakresu poprzednie wiązanie znów staje się widoczne,
- nie powinna zastępować pól domenowych ani danych, które muszą być jawne
  w kontrakcie metody.

Przykładowe zagnieżdżenie:

```text
zakres z correlationId = "request-1"
└── zagnieżdżony zakres z correlationId = "request-2"
    └── po jego zakończeniu ponownie widoczne jest "request-1"
```

`get()` wywołane poza zakresem, w którym klucz jest związany, zgłasza
wyjątek. Jeżeli kod musi sprawdzić dostępność wiązania, może użyć
`isBound()`.

## 5. Scoped Values a `ThreadLocal`

| `ThreadLocal` | `ScopedValue` |
|---|---|
| Wartość jest związana z wątkiem | Wartość jest związana z zakresem wykonania |
| Zwykle można ją zmieniać | Model jest przeznaczony do przekazywania wartości tylko do odczytu |
| Trzeba uważać na czyszczenie wartości | Wiązanie kończy się razem z zakresem |
| Samo w sobie nie opisuje relacji zadań | Dobrze współpracuje z Structured Concurrency |

Scoped Values nie są zamiennikiem `ThreadLocal` w każdym przypadku. Są
przydatne szczególnie dla niezmiennego kontekstu, takiego jak identyfikator
żądania, użytkownik lub dane diagnostyczne.

## 6. Przykład z mini-banku

W ćwiczeniu utworzymy:

- rekord `RequestContext` z `correlationId` i nazwą użytkownika,
- klasę przechowującą klucz Scoped Value,
- usługę związującą kontekst i uruchamiającą zadania Lab 9,
- loader odczytujący kontekst w każdym zadaniu,
- spec Spocka sprawdzający wynik i propagację kontekstu.

Zadania będą zwracać wartości testowe, na przykład saldo, historię i
podsumowanie. Celem nie jest podłączenie eksperymentu do endpointu
produkcyjnego ani zmiana logiki mini-banku.

## 7. Co sprawdzić w Spocku

Spec powinien obejmować:

- poprawne zwrócenie wyniku wszystkich zadań,
- widoczność tego samego `correlationId` i użytkownika w zadaniach
  podrzędnych,
- brak dostępności wiązania po zakończeniu zakresu,
- izolację dwóch kolejnych żądań z różnymi kontekstami,
- propagację błędu zadania zgodnie z zachowaniem Lab 9.

Nie opieraj testu na precyzyjnych pomiarach czasu. Testuj kontrakt
widoczności kontekstu oraz wynik operacji.

## 8. Proponowane pliki ćwiczenia

Kod dopiszemy ręcznie w osobnych plikach:

```text
src/main/java/com/delta/bank/lab/java21/scoped/RequestContext.java
src/main/java/com/delta/bank/lab/java21/scoped/RequestContextHolder.java
src/main/java/com/delta/bank/lab/java21/scoped/ScopedBankDataService.java
src/main/java/com/delta/bank/lab/java21/scoped/BankDataLoader.java
src/main/java/com/delta/bank/lab/java21/scoped/BankDataResult.java
src/test/groovy/com/delta/bank/lab/java21/scoped/ScopedBankDataServiceSpec.groovy
```

Nazwy i zawartość plików zostaną doprecyzowane w instrukcji implementacji.
Nie modyfikujemy klas produkcyjnego mini-banku.

## 9. Uruchomienie testu

Po ręcznym utworzeniu klas i speca uruchom test z katalogu projektu.
W PowerShell:

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.lab.java21.scoped.ScopedBankDataServiceSpec"
```

Jeżeli używasz Git Bash:

```bash
./gradlew test --tests "com.delta.bank.lab.java21.scoped.ScopedBankDataServiceSpec"
```

Testy integracyjne mini-banku wymagają działającej testowej bazy PostgreSQL.
Jeżeli uruchomienie pełnego zestawu testów zgłasza błędy ładowania kontekstu
Springa, najpierw uruchom bazę testową:

```bash
docker compose -f docker-compose.test.yml up -d
```

## 10. Typowe błędy

### Brak `--enable-preview`

JDK 21 nie skompiluje ani nie uruchomi kodu korzystającego z preview API bez
włączenia preview na odpowiednim etapie.

### Wiązanie kontekstu po uruchomieniu zadań

Wartość musi być związana przed uruchomieniem zadań podrzędnych, aby mogły
odziedziczyć kontekst.

### Odczyt poza zakresem

`get()` wymaga aktywnego wiązania. Sprawdź granice zakresu i miejsce
wywołania, zamiast zastępować brak wartości przypadkowym domyślnym
kontekstem.

### Modyfikowanie obiektu kontekstu

Scoped Value przekazuje wartość, ale samo nie czyni jej niezmienną. Używaj
niemutowalnego rekordu i nie umieszczaj w nim współdzielonego, zmienianego
stanu.

### Traktowanie API preview jako stabilnego

API preview może ulec zmianie między wydaniami. W tym ćwiczeniu trzymamy się
JDK 21 i nie zakładamy, że kod będzie kompilować się bez zmian na nowszym
JDK.

## 11. Pytania rekrutacyjne

### Czym jest `ScopedValue`?

To mechanizm przekazywania wartości tylko do odczytu w ograniczonym zakresie
wykonania. Wartość jest wiązana z kluczem na czas działania danego zakresu.

### Jak Scoped Values współpracują ze Structured Concurrency?

Kontekst związany przed utworzeniem zadań strukturalnych może być dostępny
w zadaniach podrzędnych. Dzięki temu nie trzeba przekazywać niezmiennego
kontekstu przez każdy argument wywołania.

### Czym Scoped Value różni się od `ThreadLocal`?

`ThreadLocal` wiąże wartość z wątkiem i może być modyfikowany; Scoped Value
wiąże wartość z zakresem i jest przeznaczony do kontrolowanego, tylko do
odczytu przekazywania kontekstu.

### Czy Scoped Values były finalne w JDK 21?

Nie. W JDK 21 były API preview i wymagały jawnego włączenia preview przy
kompilacji oraz uruchamianiu.

### Czy Scoped Values zastępują argumenty metod?

Nie. Są odpowiednie dla przekrojowego, niezmiennego kontekstu. Dane
biznesowe, które stanowią wejście lub wyjście metody, nadal powinny być
przekazywane jawnie.

## Podsumowanie

```text
ScopedValue wiąże niezmienny kontekst z zakresem.
Structured Concurrency pozwala przekazać ten kontekst do powiązanych zadań.
Virtual Threads wykonują te zadania.
```

W JDK 21 `ScopedValue` jest preview, więc używamy go w tym labie jako
ćwiczenia i pamiętamy o przypięciu konfiguracji do JDK 21.
