package com.delta.bank.lab.java21.clock;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class ClockBasedAuditService {

    private final Clock clock;

    public ClockBasedAuditService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public BankOperationAudit audit(String accountNumber, String operation) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(operation, "operation must not be null");

        return new BankOperationAudit(
                accountNumber,
                operation,
                LocalDateTime.now(clock)
        );
    }
}