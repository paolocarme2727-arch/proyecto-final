package com.example.bank.accounts.expose.web;

import static com.example.bank.accounts.util.ResponseUtils.resolve;

import com.example.bank.accounts.business.AccountService;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.BalanceResponse;
import com.example.bank.accounts.expose.model.BankAccount;
import com.example.bank.accounts.mappers.BankAccountMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated account contract.
 */
@RestController
@RequiredArgsConstructor
public class AccountApiImpl implements AccountApi {

    private final AccountService accountService;
    private final BankAccountMapper accountMapper;

    /**
     * Creates an account.
     */
    @Override
    public ResponseEntity<BankAccount> createAccount(@Valid AccountRequest accountRequest) {
        return resolve(() -> accountService.create(accountRequest)
                .map(accountMapper::toApiAccount)
                .map(account -> ResponseEntity.status(HttpStatus.CREATED).body(account)));
    }

    /**
     * Lists accounts.
     */
    @Override
    public ResponseEntity<List<BankAccount>> findAllAccounts() {
        return resolve(() -> accountService.findAll()
                .map(accountMapper::toApiAccount)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Finds one account.
     */
    @Override
    public ResponseEntity<BankAccount> findAccountById(String id) {
        return resolve(() -> accountService.findById(id)
                .map(accountMapper::toApiAccount)
                .map(ResponseEntity::ok));
    }

    /**
     * Updates account data.
     */
    @Override
    public ResponseEntity<BankAccount> updateAccount(String id, @Valid AccountRequest accountRequest) {
        return resolve(() -> accountService.update(id, accountRequest)
                .map(accountMapper::toApiAccount)
                .map(ResponseEntity::ok));
    }

    /**
     * Deletes an account.
     */
    @Override
    public ResponseEntity<Void> deleteAccount(String id) {
        return resolve(() -> accountService.delete(id)
                .map(ignored -> ResponseEntity.noContent().build()));
    }

    /**
     * Returns account balance.
     */
    @Override
    public ResponseEntity<BalanceResponse> getAccountBalance(String id) {
        return resolve(() -> accountService.getBalance(id)
                .map(ResponseEntity::ok));
    }
}
