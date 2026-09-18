package com.delta.bank.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
/*
To jest DTO (Data Transfer Object), ponieważ:
 - przenosi dane między warstwami aplikacji,
 - nie zawiera logiki biznesowej,
 - reprezentuje odpowiedź API (DepositRequest).

W tym przypadku jest to typ "record" w Javie bardzo dobrze pasuje do DTO, bo daje:
 - niemutowalność, czyli pola są finalne i nie można ich zmienić po utworzeniu obiektu,
 - zwięzłą definicję,
 - automatycznie wygenerowane accessory, equals(), hashCode() i toString().
* */
public record DepositRequest(
        @NotBlank String requestId,

        @Positive(message = "Deposit amount must be greater than zero")
        String amount
) {
}