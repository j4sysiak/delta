package com.delta.bank.lab.java21.clock;

import java.time.LocalDateTime;

public record BankOperationAudit(
        String accountNumber,
        String operation,
        LocalDateTime occurredAt
) {
}