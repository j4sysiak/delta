package com.delta.bank.lab.java21.resources;

import java.util.Objects;

public class StatementReader {

    public String readAndClose(StatementSession session) {
        Objects.requireNonNull(session, "session must not be null");

        // try-with-resources automatycznie zamknie sesję po odczycie
        try (session) {
            return session.readStatement();
        }
    }

    // przykład, który pokazuje, że jeśli wystąpi wyjątek w readStatement(), sesja nie zostanie zamknięta automatycznie
    public String readAndClose2(StatementSession session) {
        Objects.requireNonNull(session, "session must not be null");
        // Error będzie, bo sesja nie zostanie zamknięta automatycznie, jeśli wystąpi wyjątek w readStatement()
        return session.readStatement();
    }

    public void failAndClose(StatementSession session) {
        Objects.requireNonNull(session, "session must not be null");

        try (session) {
            throw new IllegalStateException("Statement processing failed");
        }
    }
}