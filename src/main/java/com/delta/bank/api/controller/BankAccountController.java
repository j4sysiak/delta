package com.delta.bank.api.controller;

import com.delta.bank.api.dto.*;
import com.delta.bank.application.service.BankAccountService;
import com.delta.bank.domain.BankAccountEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Accounts")
@RestController
@RequestMapping("/accounts")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @Operation(summary = "Create bank account")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        BankAccountEntity entity = service.openAccount(
                request.number(),
                request.owner(),
                new BigDecimal(request.balance()),
                request.currency()
        );

        return new AccountResponse(
                entity.getNumber(),
                entity.getOwner(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Operation(summary = "Read account by number")
    @GetMapping("/{number}")
    public AccountResponse getAccount(@PathVariable String number) {
        BankAccountEntity entity = service.find(number);

        return new AccountResponse(
                entity.getNumber(),
                entity.getOwner(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Operation(summary = "Deposit funds")
    @PostMapping("/{number}/deposit")
    public TransactionResultResponse deposit(@PathVariable String number, @Valid @RequestBody DepositRequest request) {
        BankAccountEntity entity = service.deposit(request.requestId(), number, new BigDecimal(request.amount()));

        return new TransactionResultResponse(
                true,
                request.requestId(),
                entity.getNumber(),
                entity.getBalance(),
                new BigDecimal(request.amount())
        );
    }

    @Operation(summary = "Withdraw funds")
    @PostMapping("/{number}/withdraw")
    public TransactionResultResponse withdraw(@PathVariable String number, @Valid @RequestBody WithdrawRequest request) {
        BankAccountEntity entity = service.withdraw(request.requestId(), number, new BigDecimal(request.amount()));

        return new TransactionResultResponse(
                true,
                request.requestId(),
                entity.getNumber(),
                entity.getBalance(),
                new BigDecimal(request.amount())
        );
    }

    @Operation(summary = "Transfer funds between accounts")
    @PostMapping("/transfer")
    public TransactionResultResponse transfer(@Valid @RequestBody TransferRequest request) {
        boolean executed = service.transfer(
                request.requestId(),
                request.fromAccount(),
                request.toAccount(),
                new BigDecimal(request.amount())
        );

        return new TransactionResultResponse(
                executed,
                request.requestId(),
                request.fromAccount(),
                service.find(request.fromAccount()).getBalance(),
                new BigDecimal(request.amount())
        );
    }
}