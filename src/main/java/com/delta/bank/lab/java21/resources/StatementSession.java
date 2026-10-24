package com.delta.bank.lab.java21.resources;

public class StatementSession implements AutoCloseable {

    private boolean closed;

    public String readStatement() {
        if (closed) {
            throw new IllegalStateException("Session is closed");
        }

        return "Statement for PLN-1001";
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}