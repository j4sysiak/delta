package com.delta.bank.lab.java21.switchpatterns;

import java.math.BigDecimal;

public class BankEventDescriber {

    private static final BigDecimal REVIEW_THRESHOLD =
            new BigDecimal("1000.00");

    public String describe(BankEvent event) {

        return switch (event) {

            case null -> "No bank event";

            case DepositRecorded(var accountNumber, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    STR."Large deposit to \{accountNumber}: \{amount.toPlainString()} PLN";

            case DepositRecorded(var accountNumber, var amount) ->
                    STR."Deposit to \{accountNumber}: \{amount.toPlainString()} PLN";

            case WithdrawalRecorded(var accountNumber, var amount)
                    when amount.compareTo(REVIEW_THRESHOLD) >= 0 ->
                    STR."Large withdrawal from \{accountNumber}: \{amount.toPlainString()} PLN";

            case WithdrawalRecorded(var accountNumber, var amount) ->
                    STR."Withdrawal from \{accountNumber}: \{amount.toPlainString()} PLN";

            case TransferRecorded(var source, var target, var amount) ->
                    STR."Transfer from \{source} to \{target}: \{amount.toPlainString()} PLN";

        };
    }
}