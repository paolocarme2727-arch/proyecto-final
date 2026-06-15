package com.example.bank.wallets.business;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import com.example.bank.wallets.expose.model.WalletRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * Yanki wallet use cases exposed through RxJava types.
 */
public interface WalletService {

    /**
     * Creates a wallet.
     */
    Single<Wallet> create(WalletRequest request);

    /**
     * Lists all wallets.
     */
    Flowable<Wallet> findAll();

    /**
     * Finds a wallet by id.
     */
    Single<Wallet> findById(String id);

    /**
     * Links a wallet to a debit card.
     */
    Single<Wallet> linkDebitCard(String id, DebitCardLinkRequest request);

    /**
     * Sends a wallet payment by phone number.
     */
    Single<WalletPaymentResponse> sendPayment(WalletPaymentRequest request);
}


