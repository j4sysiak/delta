Lab 9 jest przede wszystkim kontynuacją Lab 3 — Virtual Threads.
Oba ćwiczą równoległe pobieranie salda, historii i podsumowania. 

Różnica polega na sposobie zarządzania zadaniami:
- Lab 3: ręcznie tworzysz executor, przechowujesz Future i zbierasz wyniki.
- Lab 9: grupujesz zadania w StructuredTaskScope, który porządkuje ich cykl życia i obsługę błędów.

Natomiast:
- Lab 4 — Concurrency jest powiązany tematycznie, ale dotyczy innego problemu: ochrony wspólnego, modyfikowanego stanu przed race condition. 
- Lab 9 nie zastępuje synchronizacji z Lab 4.

Najkrócej: 
- Lab 3 uczy uruchamiania zadań na virtual threads; 
- Lab 9 pokazuje, jak takie zadania organizować strukturalnie.


# Lab 9 — Java 21: Structured Concurrency

## Cel laba

W tym labie poznajemy Structured Concurrency w wersji dostępnej w JDK 21.
Wykorzystamy ją do uruchomienia kilku powiązanych zadań bankowych jako
jednej grupy:

```text
jedno zadanie nadrzędne
├── pobierz saldo
├── pobierz historię transakcji
└── pobierz podsumowanie
```

Nauczymy się:

- tworzyć zadania przez `StructuredTaskScope`,
- uruchamiać je przez `fork(...)`,
- czekać na zakończenie przez `join()`,
- pobierać wyniki z `Subtask`,
- używać `ShutdownOnFailure`,
- rozumieć anulowanie pozostałych zadań po błędzie,
- porównywać structured concurrency z ręcznym zarządzaniem `Future`.

To będzie osobne ćwiczenie. Nie zmieniamy logiki mini-banku.

## Ważne: API Structured Concurrency w JDK 21 jest preview

Structured Concurrency w Java 21 było funkcją preview. Oznacza to, że:

- trzeba jawnie włączyć obsługę preview podczas kompilacji,
- trzeba włączyć preview także podczas uruchamiania testów,
- API preview może zmieniać się pomiędzy wydaniami JDK,
- kod używający tego API nie jest przenośny bez uwzględnienia wersji JDK
  i flag uruchomieniowych.

W tym labie celem jest poznanie koncepcji i historycznego API JDK 21 preview,
a nie rekomendowanie go bezwarunkowo do produkcyjnego kodu.

## 1. Problem przy ręcznym zarządzaniu zadaniami

W Lab 3 używaliśmy `ExecutorService` i `Future`:

```java
Future<String> balance = executor.submit(...);
Future<String> history = executor.submit(...);
Future<String> summary = executor.submit(...);
```

Kod musi sam:

- tworzyć executor,
- zapisywać futures,
- czekać na wyniki,
- obsługiwać błędy,
- decydować, co zrobić z pozostałymi zadaniami,
- zamykać executor.

Structured Concurrency grupuje zadania, które należą do jednej operacji.
Życie zadań jest ograniczone do bloku, który je uruchomił.

## 2. Podstawowa forma `StructuredTaskScope`

W JDK 21 preview wzorzec z `ShutdownOnFailure` wyglądał w uproszczeniu tak:

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var balance = scope.fork(() -> loadBalance(accountNumber));
    var history = scope.fork(() -> loadHistory(accountNumber));
    var summary = scope.fork(() -> loadSummary(accountNumber));

    scope.join();
    scope.throwIfFailed();

    return new BankDataResult(
            balance.get(),
            history.get(),
            summary.get()
    );
}
```

Znaczenie głównych kroków:

1. `fork(...)` zgłasza podzadanie do scope.
2. `join()` czeka na zakończenie podzadań.
3. `throwIfFailed()` zgłasza błąd, jeśli podzadanie zakończyło się wyjątkiem.
4. `Subtask.get()` pobiera wynik zakończonego podzadania.
5. zamknięcie scope kończy jego zakres i zarządza życiem podzadań.

Dokładne sygnatury i zachowanie należy sprawdzać dla wersji JDK, z którą
kompilowany jest kod preview.

## 3. `ShutdownOnFailure`

`ShutdownOnFailure` jest polityką scope, w której błąd jednego podzadania
powoduje zamknięcie pozostałych podzadań tego scope.

Przykładowy przypadek:

```text
saldo:      zakończone poprawnie
historia:   rzuciła wyjątek
summary:    nadal działa
```

Scope może zażądać anulowania pozostałych zadań, zamiast pozwolić im
niepotrzebnie działać dalej.

Anulowanie jest kooperacyjne. Kod zadania powinien poprawnie reagować na
przerwanie wątku, szczególnie podczas operacji blokujących.

## 4. `ShutdownOnSuccess`

JDK 21 preview udostępniało również politykę `ShutdownOnSuccess`.

Może być przydatna, gdy:

- uruchamiamy kilka równoważnych źródeł,
- wystarczy pierwszy poprawny wynik,
- pozostałe zadania można anulować po uzyskaniu wyniku.

Przykładowy scenariusz:

```text
zapytaj replikę A
zapytaj replikę B
zapytaj replikę C
użyj pierwszego poprawnego wyniku
```

W mini-banku użyjemy `ShutdownOnFailure`, ponieważ saldo, historia
i podsumowanie są potrzebne razem.

## 5. Struktura zadania

Każde zadanie uruchomione przez `fork(...)` zwraca uchwyt typu `Subtask<T>`.

Przykład:

```java
StructuredTaskScope.Subtask<String> balance =
        scope.fork(() -> loadBalance(accountNumber));
```

Po `join()` i sprawdzeniu błędów można pobrać wynik:

```java
String value = balance.get();
```

Wartość należy odczytywać dopiero po zakończeniu podzadania i po sprawdzeniu
stanu scope.

## 6. Różnica względem `ExecutorService` i `Future`

| `ExecutorService` / `Future` | Structured Concurrency |
|---|---|
| Zadania zgłasza się do executora | Zadania należą do jawnego scope |
| Program ręcznie koordynuje futures | Scope koordynuje grupę podzadań |
| Łatwo przypadkowo zgubić zadanie | Zakres życia zadań jest powiązany z blokiem |
| Trzeba zaprojektować reakcję na błędy | Polityka scope może skoordynować błąd grupy |

Structured Concurrency nie oznacza, że wątki przestają być współbieżne.
Porządkuje natomiast ich uruchamianie, oczekiwanie i obsługę błędów.

## 7. Włączenie preview w Gradle

W JDK 21 kompilacja kodu preview wymaga opcji:

```text
--enable-preview
```

Kompilator powinien również otrzymać właściwą wersję release, np.:

```text
--release 21
```

Test JVM także musi być uruchomiony z:

```text
--enable-preview
```

Konfiguracja zależy od używanego DSL i obecnych ustawień projektu. Typowy
przykład dla Gradle Groovy DSL może wyglądać tak:

```groovy
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['--enable-preview']
}

tasks.withType(Test).configureEach {
    jvmArgs += '--enable-preview'
}
```

Przed dodaniem tych ustawień należy sprawdzić istniejącą konfigurację
`build.gradle`, aby nie nadpisać toolchaina lub argumentów kompilatora.
W ramach tego laba nie zmieniamy konfiguracji automatycznie.

## 8. Obsługa wyjątków i przerwania

Operacja oczekiwania na scope może zostać przerwana. Kod powinien respektować
przerwanie, zamiast je ignorować:

```java
catch (InterruptedException exception) {
    Thread.currentThread().interrupt();
    throw new IllegalStateException("Operation was interrupted", exception);
}
```

Zadania wykonujące operacje blokujące powinny reagować na przerwanie
właściwie dla używanego API, np. zamknąć zasoby lub zakończyć oczekiwanie.

Nie należy połykać wyjątku:

```java
catch (Exception exception) {
    // nic
}
```

Błąd powinien być jawnie obsłużony albo przekazany dalej.

## 9. Testy Spocka

Testy w tym labie powinny obejmować:

- zebranie wyników wszystkich udanych podzadań,
- propagację błędu jednego podzadania,
- zachowanie wobec pozostałych podzadań po awarii,
- obsługę przerwania,
- kompletny wynik zawierający saldo, historię i podsumowanie.

Test powinien sprawdzać przede wszystkim wynik i kontrakt grupy zadań.
Nie powinien opierać się na bardzo precyzyjnych pomiarach czasu.

## 10. Typowe błędy

### Brak flag preview

Bez `--enable-preview` kod używający preview API może się nie skompilować
albo nie uruchomić.

### Włączenie preview tylko dla kompilacji

Kompilacja i uruchomienie to osobne etapy. Test JVM również musi otrzymać
flagę `--enable-preview`.

### Odczyt wyniku przed `join()`

Najpierw trzeba poczekać na zakończenie podzadań, a dopiero potem odczytać
ich wyniki.

### Ignorowanie przerwania

Połknięcie `InterruptedException` utrudnia scope anulowanie pracy i może
prowadzić do zadań, które nie kończą się prawidłowo.

### Założenie, że preview API jest stabilne

API preview może ewoluować. Kod eksperymentalny musi być przypięty do
odpowiedniej wersji JDK i powinien być jasno oznaczony.

## 11. Pytania rekrutacyjne

### Czym jest Structured Concurrency?

To podejście, w którym współbieżne zadania są organizowane jako część
ograniczonego zakresu pracy. Zadanie nadrzędne uruchamia podzadania, czeka
na nie i koordynuje ich zakończenie oraz błędy.

### Jak `StructuredTaskScope` różni się od `ExecutorService`?

Executor uruchamia zadania, natomiast structured scope dodatkowo grupuje
powiązane podzadania i wiąże ich cykl życia z zakresem nadrzędnej operacji.

### Co robi `ShutdownOnFailure`?

Reaguje na niepowodzenie podzadania, inicjując zamknięcie pozostałych
podzadań scope.

### Czy `StructuredTaskScope` było finalnym API w JDK 21?

Nie. W JDK 21 było API preview i wymagało jawnego włączenia preview podczas
kompilacji oraz uruchamiania.

### Czy Structured Concurrency zastępuje virtual threads?

Nie. To odrębne, uzupełniające się mechanizmy. Virtual threads są lekkim
sposobem wykonywania zadań; structured concurrency organizuje grupowanie
i cykl życia zadań.

### Dlaczego trzeba respektować przerwanie?

Anulowanie zadań jest kooperacyjne. Jeżeli zadanie ignoruje przerwanie,
może nie zakończyć się szybko po błędzie innego podzadania.

## 12. Proponowane pliki ćwiczenia

Po przygotowaniu konfiguracji preview i ręcznym utworzeniu klas:

```text
src/main/java/com/delta/bank/lab/java21/structured/BankDataLoader.java
src/main/java/com/delta/bank/lab/java21/structured/BankDataResult.java
src/main/java/com/delta/bank/lab/java21/structured/StructuredBankDataService.java
src/test/groovy/com/delta/bank/lab/java21/structured/StructuredBankDataServiceSpec.groovy
```

W ćwiczeniu użyjemy trzech niezależnych odczytów:

```text
saldo
historia transakcji
podsumowanie
```

Nie należy jeszcze podłączać przykładu do produkcyjnego endpointu ani
przerabiać całego mini-banku.

## 13. Uruchomienie testu

Po ręcznym przygotowaniu konfiguracji Gradle, klas i speca uruchom:

### PowerShell

```powershell
cd C:\dev\delta
.\gradlew.bat test --tests "com.delta.bank.lab.java21.structured.StructuredBankDataServiceSpec"
```

Testowe JVM musi być uruchomione z `--enable-preview`.

Oczekiwany wynik:

```text
BUILD SUCCESSFUL
```

## Podsumowanie

W tym labie poznajemy:

```text
StructuredTaskScope
ShutdownOnFailure
ShutdownOnSuccess
fork(...)
join()
Subtask<T>
preview API w JDK 21
koordynację błędów i anulowania
```

Najważniejsza idea:

```text
virtual threads uruchamiają lekkie zadania,
structured concurrency porządkuje ich grupowanie i cykl życia.
```

Pamiętaj: API Structured Concurrency w JDK 21 jest preview, więc podczas
kompilacji i uruchamiania wymaga `--enable-preview`.
