package com.delta.bank;

import java.util.List;

public class MiniBankApp {
    public static void main(String[] args) {
        var bank = new BankService();

        var alice = new BankAccount("PLN-1001", "Alice", Money.of("1000.00", "PLN"));
        var bob = new BankAccount("PLN-2002", "Bob", Money.of("250.00", "PLN"));

        bank.transfer(alice, bob, Money.of("150.00", "PLN"));

        System.out.println("Alice balance: " + alice.balance());
        System.out.println("Bob balance: " + bob.balance());
        System.out.println("Route: " + bank.routeEvent("invoice"));

        bank.processAsync(List.of(alice, bob), account -> System.out.println("Processed: " + account.owner()));
    }
}
