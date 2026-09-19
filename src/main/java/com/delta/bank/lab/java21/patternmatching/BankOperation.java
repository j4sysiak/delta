package com.delta.bank.lab.java21.patternmatching;

public sealed interface BankOperation
        permits DepositOperation, WithdrawOperation, TransferOperation {

    String currency();

    java.math.BigDecimal amount();
}