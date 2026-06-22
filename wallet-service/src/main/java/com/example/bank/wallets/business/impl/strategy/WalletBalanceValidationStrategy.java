package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.util.CommonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates available wallet balance when the source has no linked debit card.
 */
@Component
public class WalletBalanceValidationStrategy implements WalletPaymentValidationStrategy {

    /**
     * Validates source wallet balance.
     */
    @Override
    public void validate(Wallet source, Wallet target, WalletPaymentRequest request) {
        if (!CommonUtils.hasText(source.getDebitCardId())
                && source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente en el monedero");
        }
    }
}
