package com.example.bank.wallets.expose.web;

import static com.example.bank.wallets.util.ResponseUtils.resolve;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.expose.model.Wallet;
import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.mappers.WalletMapper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements the OpenAPI-generated wallet REST contract.
 */
@RestController
@RequiredArgsConstructor
public class WalletApiImpl implements WalletApi {

    private final WalletService walletService;
    private final WalletMapper walletMapper;

    /**
     * Creates a Yanki wallet.
     */
    @Override
    public ResponseEntity<Wallet> createWallet(@Valid WalletRequest walletRequest) {
        return resolve(() -> walletService.create(walletRequest)
                .map(walletMapper::toApiWallet)
                .map(wallet -> ResponseEntity.status(HttpStatus.CREATED).body(wallet)));
    }

    /**
     * Lists wallets.
     */
    @Override
    public ResponseEntity<List<Wallet>> findAllWallets() {
        return resolve(() -> walletService.findAll()
                .map(walletMapper::toApiWallet)
                .toList()
                .map(ResponseEntity::ok));
    }

    /**
     * Finds one wallet.
     */
    @Override
    public ResponseEntity<Wallet> findWalletById(String id) {
        return resolve(() -> walletService.findById(id)
                .map(walletMapper::toApiWallet)
                .map(ResponseEntity::ok));
    }

}

