package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.expose.model.WalletRequest;
import io.reactivex.rxjava3.core.Completable;

/**
 * Validates wallet creation rules.
 */
public interface WalletCreationValidationStrategy {

    /**
     * Validates a wallet request.
     *
     * @param request wallet request
     * @return completion signal
     */
    Completable validate(WalletRequest request);
}
