package com.example.bank.credits.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.credits.expose.model.CreditProductRequest;
import com.example.bank.credits.expose.model.CreditProductType;
import com.example.bank.credits.expose.model.CustomerType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for credit product compatibility rules.
 */
class CreditProductCompatibilityValidationStrategyTest {

    private final CreditProductCompatibilityValidationStrategy strategy =
            new CreditProductCompatibilityValidationStrategy();

    @Test
    void givenPersonalCustomerWithBusinessLoan_whenValidate_thenRejectsRequest() {
        CreditProductRequest request = new CreditProductRequest(
                "customer-1",
                CustomerType.PERSONAL,
                CreditProductType.BUSINESS_LOAN,
                BigDecimal.valueOf(1000));

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
