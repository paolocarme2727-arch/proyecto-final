package com.example.bank.wallets.business.domain.impl;

import com.example.bank.wallets.business.domain.WalletDomainService;
import com.example.bank.wallets.domain.Wallet;
import com.example.bank.wallets.repository.WalletRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides shared wallet domain lookups.
 */
@Service
@RequiredArgsConstructor
public class WalletDomainServiceImpl implements WalletDomainService {

    private final WalletRepository walletRepository;

    /**
     * Finds an existing wallet or raises a not-found error.
     */
    @Override
    public Wallet findExistingWallet(String id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monedero no encontrado"));
    }

    /**
     * Finds an existing wallet by phone number.
     */
    @Override
    public Wallet findExistingWalletByPhoneNumber(String phoneNumber) {
        return walletRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Monedero no encontrado"));
    }

    /**
     * Finds an existing wallet reactively.
     */
    @Override
    public Single<Wallet> findExistingWalletReactive(String id) {
        return Single.fromCallable(() -> findExistingWallet(id))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Finds an existing wallet by phone number reactively.
     */
    @Override
    public Single<Wallet> findExistingWalletByPhoneNumberReactive(String phoneNumber) {
        return Single.fromCallable(() -> findExistingWalletByPhoneNumber(phoneNumber))
                .subscribeOn(Schedulers.io());
    }
}
