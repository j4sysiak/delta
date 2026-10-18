package com.delta.bank.lab.java21.unnamedpatterns;

public sealed interface StatementEvent
        permits DepositEvent, WithdrawalEvent, TransferEvent {
}