package com.delta.bank.api;

import jakarta.validation.constraints.Positive;

public record WithdrawRequest(
        @Positive(message = "Withdrawal amount must be greater than zero")
        String amount
) {
}