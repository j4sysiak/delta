package com.delta.bank.lab.java21.recordpatterns;

public class RecordPatternProcessor {

    public String describeUsingInstanceof(DepositCommand command) {
        if (command instanceof DepositCommand(
                AccountReference(String accountNumber),
                Money(java.math.BigDecimal amount, String currency)
        )) {
            return "ACCOUNT " + accountNumber
                    + " HAS DEPOSIT " + amount
                    + " " + currency;
        }

        return "UNKNOWN COMMAND";
    }

    public String describeUsingSwitch(DepositCommand command) {
        return switch (command) {
            case null -> "NO COMMAND";

            case DepositCommand(
                    AccountReference(String accountNumber),
                    Money(java.math.BigDecimal amount, String currency)
            ) ->
                    "DEPOSIT " + amount
                            + " " + currency
                            + " TO " + accountNumber;
        };
    }

    public String describeAccountAndMoney(DepositCommand command) {
        if (command instanceof DepositCommand(
                AccountReference(String accountNumber),
                Money(java.math.BigDecimal amount, String currency)
        )) {
            return "ACCOUNT=" + accountNumber
                    + ", AMOUNT=" + amount
                    + ", CURRENCY=" + currency;
        }

        return "NO COMMAND";
    }
}