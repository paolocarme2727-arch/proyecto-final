package com.example.bank.credits.mappers;

import com.example.bank.credits.expose.model.CreditMovement;
import com.example.bank.credits.expose.model.CreditProduct;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * Maps credit domain documents to OpenAPI response models.
 */
@Component
public class CreditProductMapper {

    /**
     * Converts a domain credit product into the generated API model.
     */
    public CreditProduct toApiProduct(com.example.bank.credits.domain.CreditProduct product) {
        return new CreditProduct()
                .id(product.getId())
                .customerId(product.getCustomerId())
                .customerType(com.example.bank.credits.expose.model.CustomerType.fromValue(product.getCustomerType().name()))
                .type(com.example.bank.credits.expose.model.CreditProductType.fromValue(product.getType().name()))
                .creditLimit(product.getCreditLimit())
                .usedAmount(product.getUsedAmount())
                .outstandingBalance(product.getOutstandingBalance())
                .overdueDebt(product.isOverdueDebt())
                .createdAt(toOffsetDateTime(product.getCreatedAt()))
                .updatedAt(toOffsetDateTime(product.getUpdatedAt()));
    }

    /**
     * Converts a domain credit movement into the generated API model.
     */
    public CreditMovement toApiMovement(com.example.bank.credits.domain.CreditMovement movement) {
        return new CreditMovement()
                .id(movement.getId())
                .creditProductId(movement.getCreditProductId())
                .type(com.example.bank.credits.expose.model.CreditMovementType.fromValue(movement.getType().name()))
                .amount(movement.getAmount())
                .resultingDebt(movement.getResultingDebt())
                .availableCredit(movement.getAvailableCredit())
                .createdAt(toOffsetDateTime(movement.getCreatedAt()));
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}

