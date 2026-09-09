package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "From account is required")
        String fromAccount,

        @NotBlank(message = "To account is required")
        String toAccount,

        @Positive(message = "Transfer amount must be greater than zero")
        String amount
) {
}