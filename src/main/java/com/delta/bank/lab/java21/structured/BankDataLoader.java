package com.delta.bank.lab.java21.structured;

public interface BankDataLoader {

    String loadBalance(String accountNumber);

    String loadHistory(String accountNumber);

    String loadSummary(String accountNumber);
}