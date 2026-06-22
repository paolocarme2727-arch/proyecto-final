package com.example.bank.credits.business.domain;

import com.example.bank.credits.domain.CreditProduct;
import io.reactivex.rxjava3.core.Single;

/**
 * Defines shared credit product domain lookups.
 */
public interface CreditProductDomainService {

    /**
     * Finds an existing credit product or raises a not-found error.
     *
     * @param id credit product identifier
     * @return existing credit product
     */
    CreditProduct findExistingProduct(String id);

    /**
     * Finds an existing credit product reactively.
     *
     * @param id credit product identifier
     * @return existing credit product
     */
    Single<CreditProduct> findExistingProductReactive(String id);
}
