package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TransferRequest(
        @NotBlank(message = "Source account is required")
        String fromAccount,

        @NotBlank(message = "Destination account is required")
        String toAccount,

        @NotBlank(message = "Amount is required")
        @Pattern(regexp = "^[0-9]+(\\.[0-9]{2})?$", message = "Amount must be a valid decimal value, e.g. 100.00")
        String amount
) {
}