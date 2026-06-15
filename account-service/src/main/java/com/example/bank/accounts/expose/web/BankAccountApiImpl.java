package com.example.bank.accounts.expose.web;

import com.example.bank.accounts.business.BankAccountService;
import com.example.bank.accounts.mappers.BankAccountMapper;
import com.example.bank.accounts.expose.model.AccountMovement;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.BalanceResponse;
import com.example.bank.accounts.expose.model.BankAccount;
import com.example.bank.accounts.expose.model.DebitCard;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implements the OpenAPI-generated account REST contract.
 */
@RestController
@RequiredArgsConstructor
public class BankAccountApiImpl implements BankAccountApi {

    private final BankAccountService accountService;
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
     * Deposits money.
     */
    @Override
    public ResponseEntity<AccountMovement> depositAccount(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> accountService.deposit(id, moneyRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Withdraws money.
     */
    @Override
    public ResponseEntity<AccountMovement> withdrawAccount(String id, @Valid MoneyRequest moneyRequest) {
        return resolve(() -> accountService.withdraw(id, moneyRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    /**
     * Returns account balance.
     */
    @Override
    public ResponseEntity<BalanceResponse> getAccountBalance(String id) {
        return resolve(() -> accountService.getBalance(id)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists account movements.
     */
    @Override
    public ResponseEntity<List<AccountMovement>> findAccountMovements(String id) {
        return resolve(() -> accountService.findMovements(id)
                .map(accountMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Transfers money between accounts from the same bank.
     */
    @Override
    public ResponseEntity<TransferResponse> transferBetweenAccounts(@Valid TransferRequest transferRequest) {
        return resolve(() -> accountService.transfer(transferRequest)
                .map(ResponseEntity::ok));
    }

    /**
     * Returns a product report in a user-provided time range.
     */
    @Override
    public ResponseEntity<AccountProductReportResponse> getAccountProductReport(
            AccountType type,
            OffsetDateTime from,
            OffsetDateTime to) {
        return resolve(() -> accountService.getProductReport(type, from, to)
                .map(ResponseEntity::ok));
    }

    /**
     * Lists the last 10 movements associated with a debit-card account.
     */
    @Override
    public ResponseEntity<List<AccountMovement>> findRecentDebitCardMovements(String id) {
        return resolve(() -> accountService.findRecentDebitCardMovements(id)
                .map(accountMapper::toApiMovement)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Creates a debit card linked to a bank account.
     */
    @Override
    public ResponseEntity<DebitCard> createDebitCard(@Valid DebitCardRequest debitCardRequest) {
        return resolve(() -> accountService.createDebitCard(debitCardRequest)
                .map(accountMapper::toApiDebitCard)
                .map(card -> ResponseEntity.status(HttpStatus.CREATED).body(card)));
    }

    /**
     * Pays with a debit card.
     */
    @Override
    public ResponseEntity<AccountMovement> payWithDebitCard(
            String id,
            @Valid DebitCardPaymentRequest debitCardPaymentRequest) {
        return resolve(() -> accountService.payWithDebitCard(id, debitCardPaymentRequest)
                .map(accountMapper::toApiMovement)
                .map(ResponseEntity::ok));
    }

    private static <T> ResponseEntity<T> resolve(Supplier<Single<ResponseEntity<T>>> responseSupplier) {
        try {
            return responseSupplier.get().blockingGet();
        } catch (RuntimeException error) {
            ResponseStatusException exception = findStatusException(error);
            if (exception != null) {
                return ResponseEntity.status(exception.getStatusCode()).build();
            }
            throw error;
        }
    }

    private static ResponseStatusException findStatusException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }
}

