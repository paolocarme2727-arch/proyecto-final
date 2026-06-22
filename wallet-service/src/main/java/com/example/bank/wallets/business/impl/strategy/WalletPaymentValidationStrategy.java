package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;

/**
 * Validates wallet payment rules.
 */
public interface WalletPaymentValidationStrategy {

    /**
     * Validates a wallet payment.
     *
     * @param source source wallet
     * @param target target wallet
     * @param request payment request
     */
    void validate(Wallet source, Wallet target, WalletPaymentRequest request);
}
