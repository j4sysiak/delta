package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record WithdrawRequest(
        @NotBlank String requestId,

        @Positive(message = "Withdrawal amount must be greater than zero")
        String amount
) {
}