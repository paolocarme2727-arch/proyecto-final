package com.example.bank.wallets.expose.web;

import static com.example.bank.wallets.util.ResponseUtils.resolve;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.Wallet;
import com.example.bank.wallets.mappers.WalletMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated wallet debit-card REST contract.
 */
@RestController
@RequiredArgsConstructor
public class WalletDebitCardApiImpl implements WalletDebitCardApi {

    private final WalletService walletService;
    private final WalletMapper walletMapper;

    /**
     * Links a wallet to a debit card.
     */
    @Override
    public ResponseEntity<Wallet> linkDebitCard(String id, @Valid DebitCardLinkRequest debitCardLinkRequest) {
        return resolve(() -> walletService.linkDebitCard(id, debitCardLinkRequest)
                .map(walletMapper::toApiWallet)
                .map(ResponseEntity::ok));
    }
}
