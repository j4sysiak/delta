package com.delta.bank.lab.java21.http;

public class BankApiException extends RuntimeException {

    private final int statusCode;

    public BankApiException(int statusCode) {
        super("Bank API returned HTTP status: " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}