# Lab 14 — Streamy krok po kroku

## Cel

Przypominamy dogłębnie Stream API na przykładach transakcji mini-banku.
Pracujemy na Java 21, z testami Groovy/Spock. Nie zmieniamy kodu
produkcyjnego, nie uruchamiamy Springa ani bazy danych.

Stream API nie jest nowością Java 21. Ten lab porządkuje podstawy oraz
wykorzystuje API dostępne w używanym przez nas JDK.

Każda część ma własne README. Kod wklejasz samodzielnie.
W kolejnych częściach będziemy korzystać ze wspólnego modelu z Lab14.1.

## Kolejność

| Część | Temat | Zadanie bankowe |
|---|---|---|
| Lab14.1 | Źródło, pipeline, leniwe wykonanie, jednorazowość | Pobierz kwoty wpłat |
| Lab14.2 | `filter`, `map`, `flatMap` | Filtruj transakcje i spłaszczaj historie kont |
| Lab14.3 | `sorted`, `distinct`, `limit`, `skip`, wyszukiwanie i dopasowanie | Wybierz transakcje i sprawdź warunki |
| Lab14.4 | `count`, `min`, `max`, `reduce`, `Optional` | Oblicz sumę i znajdź skrajne kwoty |
| Lab14.5 | `toMap`, `groupingBy`, `partitioningBy`, `mapping`, `teeing` | Zbuduj zestawienia i podsumowania |
| Lab14.6 | Efekty uboczne, `peek`, kolejność operacji, strumienie równoległe | Rozpoznaj błędy i wybierz bezpieczny pipeline |

Temat `BigDecimal.equals()` kontra `compareTo()` wróci przy `distinct`
i porównywaniu kwot. Nie będziemy traktować `parallelStream()` jako
automatycznego sposobu przyspieszania kodu.

## Sposób pracy

1. Przeczytaj krótki przykład.
2. Przewidź wynik przed uruchomieniem.
3. Wklej kod i spec.
4. Uruchom tylko spec danej części.
5. Wykonaj eksperyment i wyjaśnij wynik własnymi słowami.

Materiały:

- `Readme_Lab14_1_stream_basics.md`
- `Readme_Lab14_2_filter_map_flatMap.md`
