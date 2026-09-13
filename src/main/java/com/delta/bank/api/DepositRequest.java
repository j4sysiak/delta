package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
        @NotBlank String requestId,

        @Positive(message = "Deposit amount must be greater than zero")
        String amount
) {
}