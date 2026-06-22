package com.example.bank.credits.business.impl.strategy;

import com.example.bank.credits.business.util.CreditBusinessUtils;
import com.example.bank.credits.expose.model.CreditProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates that credit limit can cover the current debt.
 */
@Component
public class CreditLimitValidationStrategy implements CreditProductShapeValidationStrategy {

    /**
     * Validates that credit limit can cover the initial debt.
     */
    @Override
    public void validate(CreditProductRequest request) {
        if (request.getCreditLimit().compareTo(CreditBusinessUtils.initialDebt(request)) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El límite de crédito no puede ser menor que la deuda actual");
        }
    }
}
