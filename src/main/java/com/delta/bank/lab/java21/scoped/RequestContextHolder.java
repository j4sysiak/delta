package com.delta.bank.lab.java21.scoped;

import java.util.Objects;
import java.util.function.Supplier;

public final class RequestContextHolder {

    // ScopedValue przechowuje bieżący RequestContext tylko w zakresie wykonania danego bloku kodu.
    // To jest uchwyt do kontekstu żądania — wartość będzie ustawiana tymczasowo przez withContext(...)
    // i odczytywana później przez current().
    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

    private RequestContextHolder() {
    }

    public static RequestContext current() {

        // Zwraca aktualnie powiązany RequestContext z ScopedValue.
        // Zadziała tylko wtedy, gdy kontekst został wcześniej ustawiony przez withContext(...).
        // Jeśli nie ma aktywnego powiązania, wywołanie zakończy się błędem.
        return REQUEST_CONTEXT.get();
    }

    public static boolean isBound() {
        return REQUEST_CONTEXT.isBound();
    }

    // Czyli withContext(...) to po prostu bezpieczny sposób powiedzenia: „uruchom ten fragment kodu z tym kontekstem”.
    // W praktyce możesz to czytać jak taki pseudokod:
    // 1. zanim kod lambdy operation się wykona, withContext(...) ustawia: REQUEST_CONTEXT = context
    // 2. wykonaj operation  ---------->     () -> loadWithinContext(accountNumber)
    // 2a. w tym czasie, jeśli w kodzie operation wywołasz RequestContextHolder.current(),
    //     to dostaniesz ten context, który został ustawiony w kroku 1
    //
    //            private Account loadWithinContext(String accountNumber) {
    //                RequestContext ctx = RequestContextHolder.current();
    //            }
    //
    // 3. zwróć wynik operation
    // 4. na końcu usuń powiązanie

    public static <T> T withContext(
            RequestContext context,
            Supplier<T> operation
    ) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        /*
        Tymczasowo wiąże podany context ze ScopedValue na czas wykonania operation i zwraca wynik działania suppliera.
        Bardziej po ludzku:
         - REQUEST_CONTEXT — to „schowek” na aktualny RequestContext, czyli context, który jest aktualnie używany w danym wątku
         - context — wartość, którą wkładamy do tego schowka
         - operation — kod, który ma się wykonać w tym kontekście  (kod lambdy przekazany jako parametr   () -> loadWithinContext(accountNumber))

        Najprościej:
        zmienna `operation` to jest kawałek kodu przekazany jako parametr do tej metody, jest typu Supplier<T>,
        czyli coś, co dostarcza wartość typu T.
        W tym przypadku, operation to jest lambda, która wywołuje metodę loadWithinContext(accountNumber) i zwraca wynik tego wywołania.
        */
        /*
        W praktyce działa to tak:
         1. REQUEST_CONTEXT zostaje powiązany z przekazanym context
         2. wykonywany jest operation.get()
         3. w środku tej lambdy `RequestContextHolder.current()` zwróci właśnie ten context
         4. po zakończeniu powiązanie znika i nie „wycieka” poza ten blok

         Biznesowo/procesowo: ta linia mówi: „wykonaj ten kawałek procesu w kontekście konkretnego żądania/operacji”.
         W praktyce chodzi o to, żeby cały przebieg obsługi jednego requestu miał dostęp do wspólnych danych, np.:
          - ID żądania / korelacji do logów,
          - użytkownika lub klienta,
          - kanału wywołania,
          - uprawnień,
          - tenantu / oddziału / sesji.

          „na czas wykonania operation ustaw bieżący RequestContext = context”

Dzięki temu kod niżej w procesie nie musi dostawać RequestContext jako parametru w każdej metodzie
             — po prostu wywołuje RequestContextHolder.current() i wie, dla jakiego requestu teraz pracuje.

Procesowo:
1. Przychodzi żądanie.
2. Tworzony jest dla niego RequestContext.
3. Logika biznesowa jest uruchamiana w ramach withContext(context, operation).
4. Na czas wykonania operation bieżący RequestContext jest dostępny przez RequestContextHolder.current().
5. Cały kod wykonywany w tym zakresie „widzi”, jaki request jest aktualnie obsługiwany.
6. Metoda zwraca wynik operation.get().
7. Po zakończeniu wykonania powiązanie kontekstu znika i nie wycieka poza ten zakres.

Procesowo Opis techniczny:
Działanie withContext(...):
1. Wiąże REQUEST_CONTEXT z przekazanym context.
2. Wykonuje operation.get().
3. W trakcie wykonania operation kod może odczytać kontekst przez RequestContextHolder.current().
4. Zwraca wynik operation.get().
5. Po zakończeniu usuwa powiązanie kontekstu.
*/

        return ScopedValue.getWhere(REQUEST_CONTEXT, context, operation);
        // lub czysto technicznie: return operation.get();
    }
}

