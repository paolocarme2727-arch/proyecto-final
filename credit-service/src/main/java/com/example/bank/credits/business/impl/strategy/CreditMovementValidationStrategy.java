package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.util.enums.CreditMovementType;
import java.math.BigDecimal;

/**
 * Validates credit movement business rules.
 */
public interface CreditMovementValidationStrategy {

    /**
     * Validates a credit movement.
     *
     * @param product credit product
     * @param amount movement amount
     * @param type movement type
     */
    void validate(CreditProduct product, BigDecimal amount, CreditMovementType type);
}
