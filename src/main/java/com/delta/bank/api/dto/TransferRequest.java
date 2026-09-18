package com.delta.bank.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
/*
To jest DTO (Data Transfer Object), ponieważ:
 - przenosi dane między warstwami aplikacji,
 - nie zawiera logiki biznesowej,
 - reprezentuje odpowiedź API (TransferRequest).

W tym przypadku jest to typ "record" w Javie bardzo dobrze pasuje do DTO, bo daje:
 - niemutowalność, czyli pola są finalne i nie można ich zmienić po utworzeniu obiektu,
 - zwięzłą definicję,
 - automatycznie wygenerowane accessory, equals(), hashCode() i toString().
* */
public record TransferRequest(
        @NotBlank(message = "Transfer request id is required")
        String requestId,

        @NotBlank(message = "From account is required")
        String fromAccount,

        @NotBlank(message = "To account is required")
        String toAccount,

        @Positive(message = "Transfer amount must be greater than zero")
        String amount
) {
}