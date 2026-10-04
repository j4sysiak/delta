package com.delta.bank.lab.java21.scoped;

public interface BankDataLoader {

    String loadBalance(String accountNumber);

    String loadHistory(String accountNumber);

    String loadSummary(String accountNumber);
}