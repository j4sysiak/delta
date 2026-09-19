package com.delta.bank.lab.java21.patternmatching;

import java.math.BigDecimal;

public record WithdrawOperation(
        String accountNumber,
        BigDecimal amount,
        String currency
) implements BankOperation {
}