package com.example.bank.accounts.business;

import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.BalanceResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Account lifecycle use cases.
 */
public interface AccountService {

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
     * Returns available balance.
     */
    Single<BalanceResponse> getBalance(String id);
}
