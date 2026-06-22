package com.example.bank.customers.business.impl.strategy;

import com.example.bank.customers.expose.model.CustomerRequest;
import io.reactivex.rxjava3.core.Completable;

/**
 * Validates customer creation rules.
 */
public interface CustomerValidationStrategy {

    /**
     * Validates a customer request.
     *
     * @param request customer request
     * @return completion signal
     */
    Completable validate(CustomerRequest request);
}
