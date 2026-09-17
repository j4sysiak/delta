package com.delta.bank.api;

import com.delta.bank.application.TransactionHistoryService;
import com.delta.bank.domain.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class TransactionController {

    private final TransactionHistoryService service;

    public TransactionController(TransactionHistoryService service) {
        this.service = service;
    }

    @GetMapping("/{number}/transactions")
    public Page<TransactionResponse> getTransactions(
            @PathVariable String number,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);

        return service.getTransactions(
                        number,
                        PageRequest.of(page, size, Sort.by(sortDirection, sortBy)),
                        type != null ? TransactionType.valueOf(type.toUpperCase()) : null,
                        from,
                        to
                )
                .map(tx -> new TransactionResponse(
                        tx.getId(),
                        tx.getAccountNumber(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getDescription(),
                        tx.getTransferRequestId(),
                        tx.getStatus(),
                        tx.getCreatedAt(),
                        tx.getUpdatedAt()
                ));
    }
}