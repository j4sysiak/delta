package com.delta.bank.api.dto;

import java.math.BigDecimal;
/*
To jest DTO (Data Transfer Object), ponieważ:
 - przenosi dane między warstwami aplikacji,
 - nie zawiera logiki biznesowej,
 - reprezentuje odpowiedź API (TransactionResultResponse).

W tym przypadku jest to typ "record" w Javie bardzo dobrze pasuje do DTO, bo daje:
 - niemutowalność, czyli pola są finalne i nie można ich zmienić po utworzeniu obiektu,
 - zwięzłą definicję,
 - automatycznie wygenerowane accessory, equals(), hashCode() i toString().
* */
public record TransactionResultResponse(
        boolean executed,
        String requestId,
        String accountNumber,
        BigDecimal balance,
        BigDecimal amount
) {
}
