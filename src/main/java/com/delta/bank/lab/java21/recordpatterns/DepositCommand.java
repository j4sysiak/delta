package com.delta.bank.lab.java21.recordpatterns;

public record DepositCommand(
        AccountReference account,
        Money money
) {
}