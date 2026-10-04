package com.delta.bank.lab.java21.scoped;

public class ScopedValueSimpleExample {

    public static void main(String[] args) {
        var context = new RequestContext("request-1", "anna");

        System.out.println(
                STR."Przed operacja: \{RequestContextHolder.isBound()}"
        );

        // Uruchamiamy operację (ta lambda) w powiązanym kontekście żądania.
        // Wewnątrz lambdy RequestContextHolder jest aktywny, więc metoda readUsername()
        // może odczytać dane kontekstowe bez przekazywania ich w argumentach.
        String username = RequestContextHolder.withContext(context, () -> {
            System.out.println(
                    STR."W trakcie operacji: \{RequestContextHolder.isBound()}"
            );
            /*
             * W tym miejscu RequestContextHolder jest aktywny, więc readUsername() może odczytać kontekst żądania.
             * Gdybyśmy wywołali readUsername() poza lambdą, to RequestContextHolder nie byłby aktywny
             * i metoda by się nie powiodła.
            **/
            return readUsername();
        });

        System.out.println(
                STR."Po operacji: \{RequestContextHolder.isBound()}"
        );
        System.out.println("Zwrocony wynik: " + username);
    }

    private static String readUsername() {
        // Metoda nie dostaje kontekstu jako argumentu i nie wie, że jest w kontekście żądania.
        // tutaj odczytujemy kontekst żądania z RequestContextHolder, który jest aktywny tylko w obrębie withContext(...).
        var context = RequestContextHolder.current();

        System.out.println("Id zadania: " + context.correlationId());
        System.out.println("Uzytkownik: " + context.username());

        return context.username();
        // lub return context.correlationId();
    }
}