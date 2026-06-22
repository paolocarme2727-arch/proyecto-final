package com.example.bank.accounts.business.domain;

import com.example.bank.accounts.domain.BankAccount;
import io.reactivex.rxjava3.core.Single;

/**
 * Defines shared account domain lookups for the business layer.
 */
public interface AccountDomainService {

    /**
     * Finds an existing account or raises a not-found error.
     *
     * @param id account identifier
     * @return existing bank account
     */
    BankAccount findExistingAccount(String id);

    /**
     * Finds an existing account reactively or raises a not-found error.
     *
     * @param id account identifier
     * @return existing bank account
     */
    Single<BankAccount> findExistingAccountReactive(String id);
}
