package com.delta.bank.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WithdrawRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9]+(\\.[0-9]{2})?$")
        String amount
) {
}