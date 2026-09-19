package com.delta.bank.lab.java21.patternmatching;

import java.math.BigDecimal;

public record TransferOperation(
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}