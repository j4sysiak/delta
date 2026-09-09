package com.delta.bank.api;

import com.delta.bank.application.BankAccountService;
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

    @Operation(summary = "Create a bank account")
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
                entity.getCurrency()
        );
    }

    @Operation(summary = "Read bank account using number")
    @GetMapping("/{number}")
    public AccountResponse getAccount(@PathVariable String number) {
        BankAccountEntity entity = service.find(number);

        return new AccountResponse(
                entity.getNumber(),
                entity.getOwner(),
                entity.getBalance(),
                entity.getCurrency()
        );
    }

    @Operation(summary = "Create a deposit transaction for a bank account")
    @PostMapping("/{number}/deposit")
    public AccountResponse deposit(@PathVariable String number, @Valid @RequestBody DepositRequest request) {
        BankAccountEntity entity = service.deposit(number, new BigDecimal(request.amount()));

        return new AccountResponse(
                entity.getNumber(),
                entity.getOwner(),
                entity.getBalance(),
                entity.getCurrency()
        );
    }

    @Operation(summary = "Create a withdraw transaction for a bank account")
    @PostMapping("/{number}/withdraw")
    public AccountResponse withdraw(@PathVariable String number, @Valid @RequestBody WithdrawRequest request) {
        BankAccountEntity entity = service.withdraw(number, new BigDecimal(request.amount()));

        return new AccountResponse(
                entity.getNumber(),
                entity.getOwner(),
                entity.getBalance(),
                entity.getCurrency()
        );
    }

    @Operation(summary = "Create a transfer transaction between bank accounts")
    @PostMapping("/transfer")
    public void transfer(@Valid @RequestBody TransferRequest request) {
        service.transfer(
                request.fromAccount(),
                request.toAccount(),
                new BigDecimal(request.amount())
        );
    }
}