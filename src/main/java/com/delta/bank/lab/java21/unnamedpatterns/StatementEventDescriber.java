package com.delta.bank.lab.java21.unnamedpatterns;

import java.math.BigDecimal;

public class StatementEventDescriber {

    private static final BigDecimal REVIEW_THRESHOLD =
            new BigDecimal("1000.00");

    public String describe(StatementEvent event) {
        return switch (event) {
            case null -> "No statement event";
            case DepositEvent(_, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    STR."Large deposit: \{amount.toPlainString()} PLN";
            case DepositEvent(_, var amount) ->
                    STR."Deposit: \{amount.toPlainString()} PLN";
            case WithdrawalEvent(_, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    STR."Large withdrawal: \{amount.toPlainString()} PLN";
            case WithdrawalEvent(_, var amount) ->
                    STR."Withdrawal: \{amount.toPlainString()} PLN";
            case TransferEvent(_, _, var amount) ->
                    STR."Transfer: \{amount.toPlainString()} PLN";
        };
    }
}