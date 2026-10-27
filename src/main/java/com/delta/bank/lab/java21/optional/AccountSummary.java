package com.delta.bank.lab.java21.optional;

import java.util.Objects;
import java.util.Optional;

public record AccountSummary(
        String accountNumber,
        String owner,
        Optional<String> alias  // to ważne, ponieważ alias może być obecny lub nie
                                // użycie Optional<String> pozwala na wyrażenie tej nieobecności w sposób bezpieczny i czytelny
                                // potem w kodzie, można użyć metod Optional, takich jak:
                                // isPresent(), ifPresent(), orElse(), aby obsłużyć sytuacje, gdy alias jest obecny lub nie

) {
    public AccountSummary {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(alias, "alias must not be null");

        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }

        // ponieważ alias jest typu Optional<String>, możemy użyć metody ifPresent() do sprawdzenia,
        // czy wartość jest obecna i nie jest pusta
        alias.ifPresent(value -> {
            if (value.isBlank()) {
                throw new IllegalArgumentException("alias must not be blank");
            }
        });
    }
}

/*

    public void ifPresent(Consumer<? super T> action) {
        if (value != null) {
            action.accept(value);
        }
    }


  @FunctionalInterface
  public interface Consumer<T> {
    void accept(T t);
  }

  Consumer<T> consumer = new Consumer<T>() {
    @Override
    public void accept(T t) {
        // implementation
    }
  };

  i ostatecznie można to zapisać jako wyrażenie lambda:
  Consumer<String> consumerLambda = t -> {
              // implementation
               if (t.isBlank()) {
                throw new IllegalArgumentException("alias must not be blank");  // zwraca wyjątek, jeśli alias jest pusty
               }
  };

* */