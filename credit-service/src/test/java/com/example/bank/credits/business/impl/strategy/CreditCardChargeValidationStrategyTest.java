package com.example.bank.credits.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.credits.domain.CreditProduct;
import com.example.bank.credits.util.enums.CreditMovementType;
import com.example.bank.credits.util.enums.CreditProductType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for credit card charge rules.
 */
class CreditCardChargeValidationStrategyTest {

    private final CreditCardChargeValidationStrategy strategy = new CreditCardChargeValidationStrategy();

    @Test
    void givenChargeOverLimit_whenValidate_thenRejectsCharge() {
        CreditProduct product = CreditProduct.builder()
                .type(CreditProductType.PERSONAL_CREDIT_CARD)
                .creditLimit(BigDecimal.valueOf(100))
                .usedAmount(BigDecimal.valueOf(80))
                .build();

        assertThatThrownBy(() -> strategy.validate(product, BigDecimal.valueOf(30), CreditMovementType.CHARGE))
                .isInstanceOf(ResponseStatusException.class);
    }
}
