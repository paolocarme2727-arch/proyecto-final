package com.example.bank.wallets.business.domain;

import com.example.bank.wallets.domain.Wallet;
import io.reactivex.rxjava3.core.Single;

/**
 * Defines shared wallet domain lookups.
 */
public interface WalletDomainService {

    /**
     * Finds an existing wallet or raises a not-found error.
     *
     * @param id wallet identifier
     * @return existing wallet
     */
    Wallet findExistingWallet(String id);

    /**
     * Finds an existing wallet by phone number.
     *
     * @param phoneNumber phone number
     * @return existing wallet
     */
    Wallet findExistingWalletByPhoneNumber(String phoneNumber);

    /**
     * Finds an existing wallet reactively.
     *
     * @param id wallet identifier
     * @return existing wallet
     */
    Single<Wallet> findExistingWalletReactive(String id);

    /**
     * Finds an existing wallet by phone number reactively.
     *
     * @param phoneNumber phone number
     * @return existing wallet
     */
    Single<Wallet> findExistingWalletByPhoneNumberReactive(String phoneNumber);
}
