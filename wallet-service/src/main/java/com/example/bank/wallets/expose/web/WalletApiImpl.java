package com.example.bank.wallets.expose.web;

import com.example.bank.wallets.business.WalletService;
import com.example.bank.wallets.mappers.WalletMapper;
import com.example.bank.wallets.expose.model.DebitCardLinkRequest;
import com.example.bank.wallets.expose.model.Wallet;
import com.example.bank.wallets.expose.model.WalletPaymentRequest;
import com.example.bank.wallets.expose.model.WalletPaymentResponse;
import com.example.bank.wallets.expose.model.WalletRequest;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Links a wallet to a debit card.
     */
    @Override
    public ResponseEntity<Wallet> linkDebitCard(String id, @Valid DebitCardLinkRequest debitCardLinkRequest) {
        return resolve(() -> walletService.linkDebitCard(id, debitCardLinkRequest)
                .map(walletMapper::toApiWallet)
                .map(ResponseEntity::ok));
    }

    /**
     * Sends a wallet payment.
     */
    @Override
    public ResponseEntity<WalletPaymentResponse> sendWalletPayment(@Valid WalletPaymentRequest walletPaymentRequest) {
        return resolve(() -> walletService.sendPayment(walletPaymentRequest)
                .map(ResponseEntity::ok));
    }

    private static <T> ResponseEntity<T> resolve(Supplier<Single<ResponseEntity<T>>> responseSupplier) {
        try {
            return responseSupplier.get().blockingGet();
        } catch (RuntimeException error) {
            ResponseStatusException exception = findStatusException(error);
            if (exception != null) {
                return ResponseEntity.status(exception.getStatusCode()).build();
            }
            throw error;
        }
    }

    private static ResponseStatusException findStatusException(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ResponseStatusException exception) {
                return exception;
            }
            current = current.getCause();
        }
        return null;
    }
}

