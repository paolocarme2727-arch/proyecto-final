package com.example.bank.accounts.business.domain.impl;

import com.example.bank.accounts.business.domain.AccountDomainService;
import com.example.bank.accounts.domain.BankAccount;
import com.example.bank.accounts.repository.BankAccountRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides shared account domain lookups.
 */
@Service
@RequiredArgsConstructor
public class AccountDomainServiceImpl implements AccountDomainService {

    private final BankAccountRepository accountRepository;

    /**
     * Finds an existing account or raises a not-found error.
     *
     * @param id account identifier
     * @return existing bank account
     */
    @Override
    public BankAccount findExistingAccount(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
    }

    /**
     * Finds an existing account reactively or raises a not-found error.
     *
     * @param id account identifier
     * @return existing bank account
     */
    @Override
    public Single<BankAccount> findExistingAccountReactive(String id) {
        return Single.fromCallable(() -> findExistingAccount(id))
                .subscribeOn(Schedulers.io());
    }
}
