package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for minimum opening amount validation.
 */
class MinimumOpeningAmountValidationStrategyTest {

    private final AccountProperties properties = new AccountProperties(
            20,
            BigDecimal.valueOf(15),
            BigDecimal.TEN,
            20,
            BigDecimal.valueOf(3.5),
            BigDecimal.valueOf(500));
    private final MinimumOpeningAmountValidationStrategy strategy =
            new MinimumOpeningAmountValidationStrategy(properties);

    @Test
    void givenInitialBalanceBelowMinimum_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                .initialBalance(BigDecimal.ONE);

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
