package com.delta.bank;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class BankService {

    public void transfer(BankAccount from, BankAccount to, Money amount) {
        from.withdraw(amount);
        to.deposit(amount);
    }

    public String routeEvent(Object event) {
        return switch (event) {
            case BankAccount account -> "ACCOUNT:" + account.number();
            case String text -> "TEXT:" + text;
            case Money money -> "MONEY:" + money.amount() + " " + money.currency();
            case null -> "NO_EVENT";
            default -> "UNKNOWN:" + event.getClass().getSimpleName();
        };
    }

    public void processAsync(List<BankAccount> accounts, Consumer<BankAccount> consumer) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (BankAccount account : accounts) {
                executor.submit(() -> consumer.accept(account));
            }
        }
    }
}
