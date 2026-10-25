package com.delta.bank.lab.java21.immutability;

import java.util.List;
import java.util.Objects;

public record AccountStatement(
        String accountNumber,
        List<String> descriptions
) {

    // Konstruktor, który też waliduje dane wejściowe
    // Rekord ma pola final, ale to nie znaczy, że wszystko, co przechowuje, jest niemutowane
    // tzn. jeśli pole jest typu List, to można zmieniać jego zawartość, np. dodawać elementy do listy.
    public AccountStatement {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(descriptions, "descriptions must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }

        for (var description : descriptions) {
            Objects.requireNonNull(description, "description must not be null");
        }

        // Ta linia tworzy niemodyfikowalną kopię listy `descriptions` i przypisuje ją do komponentu rekordu.
        // Dzięki temu zmiany w przekazanej liście nie zmienią danych `AccountStatement` a przez accessor nie da się modyfikować listy.
        // `List.copyOf` odrzuca też elementy `null` — ale też już wcześniej są sprawdzane jawnie.

        // Czyli, aby rekord przechowywał niemodyfikowalną kopię, użyj w konstruktorze to:
        // List.copyOf() zapewnia dwa istotne zachowania:
        //  - późniejsze zmiany struktury źródłowej listy nie zmieniają wyniku,
        //  - wynikowa lista nie pozwala dodawać, usuwać ani zastępować elementów.
        descriptions = List.copyOf(descriptions);
    }
}