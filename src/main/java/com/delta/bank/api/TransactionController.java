package com.delta.bank.api;

import com.delta.bank.application.TransactionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class TransactionController {

    private final TransactionHistoryService service;

    public TransactionController(TransactionHistoryService service) {
        this.service = service;
    }

    @GetMapping("/{number}/transactions")
    public List<TransactionResponse> getTransactions(@PathVariable String number) {
        return service.getTransactions(number)
                .stream()
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getAccountNumber(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getDescription(),
                        tx.getCreatedAt()
                ))
                .toList();
    }
}