package com.delta.bank.lab.java21.streamdeepdive;

import java.math.BigDecimal;

public record TransactionGroupSummary(long count, BigDecimal total) {
}