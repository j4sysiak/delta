package com.delta.bank.lab.java21.patternmatching;

import java.math.BigDecimal;

/*
W `record` taki zapis:

```java
public record DepositOperation(
    String accountNumber,
    BigDecimal amount,
    String currency
) implements BankOperation
```

powoduje, że Java **automatycznie generuje** m.in.:

```java
private final String currency;

public String currency() {
    return currency;
}
```

Czyli:

- `String currency` w nagłówku rekordu to **deklaracja komponentu rekordu**
- z tego komponentu kompilator tworzy **pole**
- i jednocześnie tworzy **metodę akcesora** `currency()`

Więc jeśli `BankOperation` wymaga metody `currency()`, to ten `record` ją spełnia automatycznie — nie musisz jej dopisywać ręcznie.
**/


public record DepositOperation(
        String accountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}