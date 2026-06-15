package com.example.bank.accounts.business;

import com.example.bank.accounts.domain.AccountMovement;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.domain.DebitCard;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountProductReportResponse;
import com.example.bank.accounts.expose.model.BalanceResponse;
import com.example.bank.accounts.expose.model.DebitCardPaymentRequest;
import com.example.bank.accounts.expose.model.DebitCardRequest;
import com.example.bank.accounts.expose.model.MoneyRequest;
import com.example.bank.accounts.expose.model.TransferRequest;
import com.example.bank.accounts.expose.model.TransferResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.time.OffsetDateTime;

/**
 * Bank account use cases exposed through RxJava types.
 */
public interface BankAccountService {

    /**
     * Creates a bank account.
     */
    Single<BankAccount> create(AccountRequest request);

    /**
     * Lists all bank accounts.
     */
    Flowable<BankAccount> findAll();

    /**
     * Finds a bank account by id.
     */
    Single<BankAccount> findById(String id);

    /**
     * Updates account ownership data.
     */
    Single<BankAccount> update(String id, AccountRequest request);

    /**
     * Deletes a bank account.
     */
    Single<Boolean> delete(String id);

    /**
     * Registers a deposit.
     */
    Single<AccountMovement> deposit(String id, MoneyRequest request);

    /**
     * Registers a withdrawal.
     */
    Single<AccountMovement> withdraw(String id, MoneyRequest request);

    /**
     * Returns available balance.
     */
    Single<BalanceResponse> getBalance(String id);

    /**
     * Lists account movements.
     */
    Flowable<AccountMovement> findMovements(String id);

    /**
     * Transfers money between two bank accounts in the same bank.
     */
    Single<TransferResponse> transfer(TransferRequest request);

    /**
     * Builds a product report in the requested time range.
     */
    Single<AccountProductReportResponse> getProductReport(
            com.example.bank.accounts.expose.model.AccountType type,
            OffsetDateTime from,
            OffsetDateTime to);

    /**
     * Lists the last 10 debit card movements for an account.
     */
    Flowable<AccountMovement> findRecentDebitCardMovements(String id);

    /**
     * Creates a debit card associated with an account.
     */
    Single<DebitCard> createDebitCard(DebitCardRequest request);

    /**
     * Pays with a debit card using the linked account balance.
     */
    Single<AccountMovement> payWithDebitCard(String id, DebitCardPaymentRequest request);
}


