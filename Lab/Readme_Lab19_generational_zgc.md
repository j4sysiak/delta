# Lab 19 — Generational ZGC w Java 21

## Cel laba

Poznasz Generational ZGC, tryb garbage collectora ZGC wprowadzony w Java 21.
To ćwiczenie dotyczy ustawień uruchomieniowych JVM i obserwacji logów GC,
a nie nowej składni języka.

Przećwiczysz:

- czym jest garbage collector i co robi aplikacja, a co JVM,
- uruchomienie Generational ZGC w Java 21,
- odczyt podstawowych logów GC,
- porównanie konfiguracji ZGC i G1 bez wyciągania wniosków z jednego
  krótkiego pomiaru.

Nie zmieniamy produkcyjnego mini-banku ani `build.gradle`. Nie ma tu
preview API ani testu Spocka, który mógłby potwierdzić wybrany collector:
wybór collectora jest właściwością uruchomionej JVM i sprawdzamy go
w logach.

## 1. Czym zajmuje się garbage collector?

Java zarządza pamięcią obiektów na stercie. Gdy obiekt przestaje być
osiągalny z aktywnych referencji, garbage collector może odzyskać pamięć.

Kod aplikacji:

- tworzy i utrzymuje referencje do obiektów,
- nie zwalnia ręcznie pamięci obiektów.

Garbage collector:

- identyfikuje obiekty, które nie są już osiągalne,
- odzyskuje zajmowaną przez nie pamięć,
- może wpływać na zużycie CPU, przepustowość i pauzy aplikacji.

Nie należy zakładać, że obiekt zostanie zebrany natychmiast po wyjściu
z zakresu zmiennej. Garbage collection nie jest deterministycznym
odpowiednikiem `free()` ani zamykania zasobów takich jak plik czy socket.

## 2. Generational ZGC

Generational ZGC dzieli obiekty według wieku:

```text
young generation → obiekty nowe, często krótko żyjące
old generation   → obiekty, które przetrwały dłużej
```

W wielu aplikacjach dużo nowo utworzonych obiektów szybko staje się
nieosiągalnych. Generational ZGC może częściej koncentrować pracę na
obiektach młodych, a rzadziej skanować te, które żyją długo.

ZGC został zaprojektowany z myślą o krótkich pauzach, również dla dużych
ster­t. To nie jest gwarancja „braku pauz” ani automatycznie najlepszego
czasu odpowiedzi dla każdej aplikacji. Collector trzeba dobierać i mierzyć
w warunkach zbliżonych do produkcyjnych.

Generational mode w tym labie jest częścią ZGC:

```text
-XX:+UseZGC -XX:+ZGenerational
```

Nie myl jej z wyborem G1:

```text
-XX:+UseG1GC
```

Nie ustawiaj obu collectorów naraz.

## 3. Bezpieczny workload do obserwacji alokacji

Dodamy mały program, który alokuje krótkotrwałe tablice bajtów, przetwarza
je i usuwa referencje po każdej rundzie. Program ma pokazać pracę JVM,
a nie symulować dokładny profil mini-banku ani służyć do benchmarkowania.

Utwórz:

```text
src\main\java\com\delta\bank\lab\java21\gc\AllocationWorkload.java
```

Wklej:

```java
package com.delta.bank.lab.java21.gc;

import java.util.ArrayList;
import java.util.List;

public class AllocationWorkload {

    private static final int ROUNDS = 30;
    private static final int ARRAYS_PER_ROUND = 20_000;
    private static final int ARRAY_SIZE_BYTES = 1_024;

    public static void main(String[] args) {
        long checksum = 0;
        long arraysAllocated = 0;

        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> batch = new ArrayList<>(ARRAYS_PER_ROUND);

            for (int index = 0; index < ARRAYS_PER_ROUND; index++) {
                var bytes = new byte[ARRAY_SIZE_BYTES];
                bytes[0] = (byte) (round + index);
                batch.add(bytes);
            }

            for (var bytes : batch) {
                checksum += Byte.toUnsignedInt(bytes[0]);
            }

            arraysAllocated += batch.size();
        }

        System.out.println("Rounds: " + ROUNDS);
        System.out.println("Arrays allocated: " + arraysAllocated);
        System.out.println("Approximate array payload: "
                + (arraysAllocated * ARRAY_SIZE_BYTES / (1024 * 1024))
                + " MiB");
        System.out.println("Checksum: " + checksum);
    }
}
```

Po zakończeniu każdej iteracji zmienna `batch` przestaje być używana,
więc jej tablice mogą stać się nieosiągalne. JVM sama zdecyduje, kiedy
uruchomić GC. Kod nie wywołuje `System.gc()`, bo ręczne żądanie GC nie jest
wiarygodnym sposobem sterowania cyklem życia pamięci.

## 4. Uruchomienie w IntelliJ IDEA

Klasy projektu są kompilowane z `--enable-preview` ze względu na wcześniejsze
laby, dlatego uruchomienie klasy może wymagać flagi preview, nawet jeśli
ta konkretna klasa jej nie używa.

1. Otwórz `AllocationWorkload.java` i wybierz **Run**.
2. Otwórz **Run → Edit Configurations** dla `AllocationWorkload`.
3. Ustaw JDK na 21.
4. W polu **VM options** wpisz:

```text
--enable-preview -XX:+UseZGC -XX:+ZGenerational -Xms128m -Xmx128m -Xlog:gc
```

5. Uruchom program.

Sprawdź w konsoli:

- czy program wypisał liczbę rund, alokacji i checksum,
- czy JVM wypisała logi GC,
- czy w logach widnieje ZGC.

Jeśli JVM informuje, że nie zna opcji `ZGenerational`, sprawdź, czy
konfiguracja uruchomienia rzeczywiście używa JDK 21, a nie innej wersji.

## 5. Co oznaczają opcje JVM?

| Opcja | Znaczenie |
|---|---|
| `--enable-preview` | Pozwala uruchomić klasy projektu kompilowane z preview |
| `-XX:+UseZGC` | Wybiera ZGC |
| `-XX:+ZGenerational` | Włącza tryb generacyjny ZGC w JDK 21 |
| `-Xms128m` | Początkowy rozmiar sterty |
| `-Xmx128m` | Maksymalny rozmiar sterty |
| `-Xlog:gc` | Włącza podstawowe logowanie garbage collectora |

Rozmiar sterty jest ograniczony, by ćwiczenie nie alokowało bez końca.
Jeśli program nie może się uruchomić z powodu ograniczeń środowiska,
zmniejsz `ARRAYS_PER_ROUND` do `10_000`. Nie zwiększaj rozmiaru sterty
bez potrzeby.

## 6. Porównanie z G1

Uruchom ten sam program ponownie, zmieniając **tylko** collector w VM
options:

```text
--enable-preview -XX:+UseG1GC -Xms128m -Xmx128m -Xlog:gc
```

Zapisz obserwacje:

| Pytanie | ZGC | G1 |
|---|---|---|
| Jaki collector wskazują logi? | | |
| Czy pojawiły się zdarzenia GC? | | |
| Jaki był całkowity czas programu? | | |
| Czy było to kontrolowane porównanie wydajności? | | |

Ostatnia odpowiedź powinna brzmieć „nie”. Jedno krótkie uruchomienie na
maszynie developerskiej nie jest benchmarkiem. Wynik zależy m.in. od
sprzętu, obciążenia, JDK, ustawień sterty i profilu alokacji.

Nie porównuj tylko liczby komunikatów ani nie zakładaj, że krótszy log
oznacza szybszy collector.

## 7. Opcjonalnie: bardziej szczegółowe logi

Jeśli chcesz zobaczyć więcej informacji diagnostycznych, uruchom ponownie
z:

```text
-Xlog:gc*
```

Logi są zależne od wersji JVM i mogą zawierać wiele szczegółów. Na początek
wystarczy potwierdzić, który collector został uruchomiony i że workload
zakończył się prawidłowo.

## 8. Czego ten lab nie dowodzi

Ten program:

- nie mierzy produkcyjnych opóźnień,
- nie dowodzi, że Generational ZGC jest szybszy od G1,
- nie pokazuje wszystkich rodzajów obciążenia,
- nie wymusza dokładnego momentu GC,
- nie dowodzi, że wszystkie utworzone tablice zostały zebrane przed
  zakończeniem programu.

Do wiarygodnej analizy potrzebne są reprezentatywne dane, powtarzalne
warunki, narzędzia pomiarowe i analiza całego profilu aplikacji.

## 9. Pytania rekrutacyjne

### Co oznacza „generational” w Generational ZGC?

Sterta jest podzielona na generacje młodych i starych obiektów.
Collector może dopasowywać częstotliwość i zakres pracy do typowego czasu
życia obiektów.

### Czy garbage collector zwalnia zasoby zewnętrzne deterministycznie?

Nie. Pliki, połączenia i inne zasoby trzeba zamykać jawnie, zwykle przez
`try-with-resources`. Nie wolno polegać na GC w celu ich terminowego
zwolnienia.

### Czy ZGC gwarantuje brak pauz?

Nie. ZGC dąży do krótkich pauz, ale nie oznacza to braku pauz ani
gwarantowanego czasu odpowiedzi aplikacji.

### Czy Generational ZGC jest zawsze szybszy od G1?

Nie. Właściwy wybór zależy od aplikacji, sprzętu i wymagań. Trzeba
zmierzyć reprezentatywne obciążenie.

### Czy `System.gc()` gwarantuje natychmiastowe zebranie obiektów?

Nie. To tylko żądanie dla JVM, które może zostać zignorowane albo
obsłużone według decyzji collectora.

### Czy garbage collector może zamykać pliki zamiast kodu aplikacji?

Nie należy na tym polegać. Zasoby zewnętrzne trzeba zamykać jawnie.

## Do zapamiętania

```text
Java 21: Generational ZGC
-XX:+UseZGC -XX:+ZGenerational
GC zarządza pamięcią obiektów, nie zastępuje jawnego zamykania zasobów.
Logi pomagają obserwować GC, ale ten workload nie jest benchmarkiem.
```
