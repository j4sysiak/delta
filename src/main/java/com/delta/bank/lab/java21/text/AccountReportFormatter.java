package com.delta.bank.lab.java21.text;

import java.math.BigDecimal;
import java.util.Objects;

public class AccountReportFormatter {

    public String format(String accountNumber, BigDecimal balance) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(balance, "balance must not be null");

        var balanceText = balance.toPlainString();
        var report = """
                Account report
                Account: %s
                Balance: %s PLN
                """.formatted(accountNumber, balanceText);

        return report;
    }
}