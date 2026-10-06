package com.delta.bank.lab.java21.switchexpressions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class BankFeeCalculator {

    public BigDecimal calculate(BankOperationType operation, BigDecimal amount) {
        Objects.requireNonNull(operation, "operation must not be null");
        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        var fee = switch (operation) {
            case DEPOSIT -> new BigDecimal("0.00");
            case WITHDRAW -> new BigDecimal("1.00");
            case TRANSFER -> {
                var percentageFee = amount.multiply(new BigDecimal("0.005"));
                var minimumFee = new BigDecimal("2.00");
                yield percentageFee.max(minimumFee);
            }
        };

        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}