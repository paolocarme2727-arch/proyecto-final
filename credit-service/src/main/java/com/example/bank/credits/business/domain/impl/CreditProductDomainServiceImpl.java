package com.example.bank.credits.business.domain.impl;

import com.example.bank.credits.business.domain.CreditProductDomainService;
import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.repository.CreditProductRepository;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides shared credit product domain lookups.
 */
@Service
@RequiredArgsConstructor
public class CreditProductDomainServiceImpl implements CreditProductDomainService {

    private final CreditProductRepository productRepository;

    /**
     * Finds an existing credit product or raises a not-found error.
     */
    @Override
    public CreditProduct findExistingProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto de crédito no encontrado"));
    }

    /**
     * Finds an existing credit product reactively.
     */
    @Override
    public Single<CreditProduct> findExistingProductReactive(String id) {
        return Single.fromCallable(() -> findExistingProduct(id))
                .subscribeOn(Schedulers.io());
    }
}
