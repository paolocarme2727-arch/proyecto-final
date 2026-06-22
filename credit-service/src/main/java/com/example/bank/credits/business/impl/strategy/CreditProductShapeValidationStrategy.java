package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.expose.model.CreditProductRequest;

/**
 * Validates credit product request shape.
 */
public interface CreditProductShapeValidationStrategy {

    /**
     * Validates a credit product request.
     *
     * @param request credit product request
     */
    void validate(CreditProductRequest request);
}
