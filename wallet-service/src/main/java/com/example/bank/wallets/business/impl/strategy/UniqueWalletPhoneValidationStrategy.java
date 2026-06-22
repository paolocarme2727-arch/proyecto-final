package com.example.bank.wallets.business.impl.strategy;

import com.example.bank.wallets.expose.model.WalletRequest;
import com.example.bank.wallets.repository.WalletRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that a phone number has a single wallet.
 */
@Component
@RequiredArgsConstructor
public class UniqueWalletPhoneValidationStrategy implements WalletCreationValidationStrategy {

    private final WalletRepository walletRepository;

    /**
     * Validates phone uniqueness.
     */
    @Override
    public Completable validate(WalletRequest request) {
        return Single.fromCallable(() -> walletRepository.existsByPhoneNumber(request.getPhoneNumber()))
                .flatMapCompletable(exists -> exists
                        ? Completable.error(new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El número de celular ya tiene un monedero"))
                        : Completable.complete());
    }
}
