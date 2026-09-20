package com.delta.bank.lab.java21.concurrency;

import java.math.BigDecimal;

// Interfejs pozwala uruchomić ten sam runner zarówno dla wersji niebezpiecznej, jak i bezpiecznej
public interface DepositAccount {

    void deposit(BigDecimal amount);

    BigDecimal balance();
}