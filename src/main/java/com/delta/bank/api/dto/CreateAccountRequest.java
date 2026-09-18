package com.delta.bank.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/*
To jest DTO (Data Transfer Object), ponieważ:
 - przenosi dane między warstwami aplikacji,
 - nie zawiera logiki biznesowej,
 - reprezentuje odpowiedź API (CreateAccountRequest).

W tym przypadku jest to typ "record" w Javie bardzo dobrze pasuje do DTO, bo daje:
 - niemutowalność, czyli pola są finalne i nie można ich zmienić po utworzeniu obiektu,
 - zwięzłą definicję,
 - automatycznie wygenerowane accessory, equals(), hashCode() i toString().
* */
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