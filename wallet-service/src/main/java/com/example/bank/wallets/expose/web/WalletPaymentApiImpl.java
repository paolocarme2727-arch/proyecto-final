package com.example.bank.wallets.expose.web;

import static com.example.bank.wallets.util.ResponseUtils.resolve;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated wallet payment REST contract.
 */
@RestController
@RequiredArgsConstructor
public class WalletPaymentApiImpl implements WalletPaymentApi {

    private final WalletService walletService;

    /**
     * Sends a wallet payment.
     */
    @Override
    public ResponseEntity<WalletPaymentResponse> sendWalletPayment(@Valid WalletPaymentRequest walletPaymentRequest) {
        return resolve(() -> walletService.sendPayment(walletPaymentRequest)
                .map(ResponseEntity::ok));
    }
}
