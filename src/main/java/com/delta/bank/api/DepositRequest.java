package com.delta.bank.api;

import jakarta.validation.constraints.Positive;

public record DepositRequest(
        @Positive(message = "Deposit amount must be greater than zero")
        String amount
) {
}