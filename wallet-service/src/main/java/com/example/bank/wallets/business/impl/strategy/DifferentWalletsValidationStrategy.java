package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that wallet payment endpoints are different.
 */
@Component
public class DifferentWalletsValidationStrategy implements WalletPaymentValidationStrategy {

    /**
     * Validates different source and target wallets.
     */
    @Override
    public void validate(Wallet source, Wallet target, WalletPaymentRequest request) {
        if (source.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los monederos de origen y destino deben ser distintos");
        }
    }
}
