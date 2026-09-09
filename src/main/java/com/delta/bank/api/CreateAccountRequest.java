package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
        @NotBlank(message = "Account number is required")
        String number,

        @NotBlank(message = "Owner is required")
        String owner,

        @PositiveOrZero(message = "Balance must be >= 0")
        String balance,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase code")
        String currency
) {
}