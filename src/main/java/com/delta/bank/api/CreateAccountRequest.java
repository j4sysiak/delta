package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank(message = "Account number is required")
        String number,

        @NotBlank(message = "Owner is required")
        String owner,

        @NotBlank(message = "Balance is required")
        @Pattern(regexp = "^[0-9]+(\\.[0-9]{2})?$", message = "Balance must be a valid decimal value, e.g. 1000.00")
        String balance,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
        String currency
) {
}