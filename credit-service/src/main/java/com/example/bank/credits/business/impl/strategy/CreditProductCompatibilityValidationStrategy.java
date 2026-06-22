package com.example.bank.credits.business.impl.strategy;

import static com.example.bank.credits.business.util.CreditBusinessUtils.toDomainCustomerType;
import static com.example.bank.credits.business.util.CreditBusinessUtils.toDomainProductType;

import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.util.enums.CreditProductType;
import com.example.bank.credits.util.enums.CustomerType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates compatible customer and credit product types.
 */
@Component
public class CreditProductCompatibilityValidationStrategy implements CreditProductShapeValidationStrategy {

    /**
     * Validates compatible customer and credit product types.
     */
    @Override
    public void validate(CreditProductRequest request) {
        if (toDomainCustomerType(request) == CustomerType.PERSONAL
                && (toDomainProductType(request) == CreditProductType.BUSINESS_LOAN
                || toDomainProductType(request) == CreditProductType.BUSINESS_CREDIT_CARD)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los clientes personales no pueden tener productos de crédito empresariales");
        }
        if (toDomainCustomerType(request) == CustomerType.BUSINESS
                && (toDomainProductType(request) == CreditProductType.PERSONAL_LOAN
                || toDomainProductType(request) == CreditProductType.PERSONAL_CREDIT_CARD)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los clientes empresariales no pueden tener productos de crédito personales");
        }
    }
}
