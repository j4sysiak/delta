package com.delta.bank.lab.java21.virtualthreads;

/**
 * Wynik pobrania danych bankowych: saldo, historia operacji i podsumowanie.
 */
public record BankDataResult(
        String balance,
        String history,
        String summary
) {
}