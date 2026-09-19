package com.delta.bank.lab.java21.virtualthreads;

public interface BankDataLoader {

    default String loadAccountBalance(String accountNumber) {
        return "1000.00 USD";
    }

    default String loadTransactionHistory(String accountNumber) {
        return "2024-01-01: Deposit 500.00 USD\n2024-02-01: Withdraw 200.00 USD";
    }

    default String loadAccountSummary(String accountNumber) {
        return "SUMMARY: TEST_SUMMARY";
    }
}