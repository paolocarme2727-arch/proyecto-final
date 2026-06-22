package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.expose.model.CreditProductRequest;
import io.reactivex.rxjava3.core.Completable;

/**
 * Validates credit product creation business rules.
 */
public interface CreditProductCreationValidationStrategy {

    /**
     * Validates a credit product creation request.
     *
     * @param request credit product request
     * @return completion signal
     */
    Completable validate(CreditProductRequest request);
}
