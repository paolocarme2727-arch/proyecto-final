package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.util.enums.CreditMovementType;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates payment amounts against outstanding debt.
 */
@Component
public class CreditPaymentAmountValidationStrategy implements CreditMovementValidationStrategy {

    /**
     * Validates payment amount.
     */
    @Override
    public void validate(CreditProduct product, BigDecimal amount, CreditMovementType type) {
        if (type == CreditMovementType.PAYMENT
                && product.getOutstandingBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago supera la deuda pendiente");
        }
    }
}
