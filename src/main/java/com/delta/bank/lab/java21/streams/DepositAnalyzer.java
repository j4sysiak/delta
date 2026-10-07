package com.delta.bank.lab.java21.streams;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DepositAnalyzer {

    public List<BigDecimal> atLeast(
            List<BigDecimal> deposits,
            BigDecimal minimum
    ) {
        validateDeposits(deposits);
        Objects.requireNonNull(minimum, "minimum must not be null");

        return deposits.stream()
                .filter(amount -> amount.compareTo(minimum) >= 0)
                .toList();

        /*
        @FunctionalInterface
        public interface Predicate<T> {
          boolean test(T t);
        }

        lambda expression: amount -> amount.compareTo(minimum) >= 0
        * */
    }

    public DepositSummary summarize(List<BigDecimal> deposits) {
        validateDeposits(deposits);

        // Tutaj stream z listy deposits jest podsumowywany na dwa sposoby naraz,
        // a potem wyniki są składane w DepositSummary.
        /* rozkładam na części:
 1.  deposits.stream()
     zamienia List<BigDecimal> na strumień elementów, żeby można było je przetwarzać funkcyjnie.

 2.  .collect(...)
     kończy przetwarzanie strumienia i zbiera wynik do jednej wartości końcowej.

 3.  Collectors.teeing(...)
     uruchamia dwa collectory równolegle na tym samym strumieniu.
     To znaczy: nie przechodzisz po liście dwa razy, tylko raz, a w trakcie liczysz dwie rzeczy.

 4.  Collectors.counting()
     liczy liczbę elementów w deposits.
     wynik: Long

 5.  Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
     sumuje wszystkie elementy w deposits (kwoty).

      - start od BigDecimal.ZERO
      - dla kolejnych elementów używa BigDecimal::add

     wynik: BigDecimal

  6.  DepositSummary::new
      to funkcja łącząca oba wyniki w obiekt końcowy.
      działa jak:
            (count, sum) -> new DepositSummary(count, sum)

  W uproszczonej formie: Różnica jest taka, że teeing(...) robi to w jednym collectcie, bardziej „streamowo”.

  long count = deposits.size();
  BigDecimal sum = deposits.stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);

  return new DepositSummary(count, sum);
        * */
        return deposits.stream()
                .collect(Collectors.teeing(      // ---> 1. strumień -> 2. collect -> 3. teeing
                        Collectors.counting(),   // ---> 4. counting
                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add),  // ---> 5. reducing, czyli sumowanie wszystkich elementów w strumieniu
                                                                                // 1. startuj od wartości początkowej BigDecimal.ZERO,
                                                                                // 2. bierz kolejne elementy strumienia,
                                                                                // 3. dokładaj je przez BigDecimal::add,
                                                                                // 4. na końcu zwróć jedną sumę.
                        DepositSummary::new   // ---> 6. składanie wyników w DepositSummary
                ));
    }

    private void validateDeposits(List<BigDecimal> deposits) {
        Objects.requireNonNull(deposits, "deposits must not be null");

        for (var amount : deposits) {
            Objects.requireNonNull(amount, "deposit must not be null");

            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("deposit must be positive");
            }
        }
    }
}