package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.business.util.CreditBusinessUtils;
import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.util.enums.CreditMovementType;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates credit card charges.
 */
@Component
public class CreditCardChargeValidationStrategy implements CreditMovementValidationStrategy {

    /**
     * Validates credit card charge rules.
     */
    @Override
    public void validate(CreditProduct product, BigDecimal amount, CreditMovementType type) {
        if (type != CreditMovementType.CHARGE) {
            return;
        }
        if (!CreditBusinessUtils.isCard(product.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La operación solo está permitida para tarjetas de crédito");
        }
        if (product.getUsedAmount().add(amount).compareTo(product.getCreditLimit()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se superó el límite de la tarjeta de crédito");
        }
    }
}
