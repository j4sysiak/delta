package com.delta.bank.lab.java21.strings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class AccountStatementFormatter {

    public String format(
            String accountNumber,
            String description,
            BigDecimal balance
    ) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(balance, "balance must not be null");

        var normalizedAccountNumber = accountNumber.strip();
        if (normalizedAccountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }

        var normalizedDescription = description.strip();
        if (normalizedDescription.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        var separator = "-".repeat(24);
        return """
                Account statement
                Account: %s
                Description: %s
                Balance: %s PLN
                %s
                """.formatted(
                normalizedAccountNumber,
                normalizedDescription,
                balance.toPlainString(),
                separator
        );
    }

    public List<String> lines(String statement) {
        Objects.requireNonNull(statement, "statement must not be null");

        return statement.lines().toList();
    }
}