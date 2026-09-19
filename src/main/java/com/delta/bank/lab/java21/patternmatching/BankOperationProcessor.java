package com.delta.bank.lab.java21.patternmatching;

import java.util.Objects;

public class BankOperationProcessor {

    public String process(BankOperation operation) {
        Objects.requireNonNull(operation, "operation must not be null");

        return switch (operation) {
            case DepositOperation deposit ->
                    "DEPOSIT " + deposit.amount()
                            + " " + deposit.currency()
                            + " TO " + deposit.accountNumber();

            case WithdrawOperation withdraw ->
                    "WITHDRAW " + withdraw.amount()
                            + " " + withdraw.currency()
                            + " FROM " + withdraw.accountNumber();

            case TransferOperation transfer ->
                    "TRANSFER " + transfer.amount()
                            + " " + transfer.currency()
                            + " FROM " + transfer.fromAccountNumber()
                            + " TO " + transfer.toAccountNumber();
        };
    }

    public String describeUsingInstanceof(BankOperation operation) {
        Objects.requireNonNull(operation, "operation must not be null");

        if (operation instanceof DepositOperation deposit) {
            return "Deposit for account " + deposit.accountNumber();
        }

        if (operation instanceof WithdrawOperation withdraw) {
            return "Withdraw for account " + withdraw.accountNumber();
        }

        if (operation instanceof TransferOperation transfer) {
            return "Transfer from " + transfer.fromAccountNumber()
                    + " to " + transfer.toAccountNumber();
        }

        throw new IllegalStateException("Unsupported operation: " + operation);
    }
}