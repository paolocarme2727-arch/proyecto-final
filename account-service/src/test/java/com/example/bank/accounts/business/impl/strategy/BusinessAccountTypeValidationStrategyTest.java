package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for business account type validation.
 */
class BusinessAccountTypeValidationStrategyTest {

    private final BusinessAccountTypeValidationStrategy strategy = new BusinessAccountTypeValidationStrategy();

    @Test
    void givenBusinessSavingsAccount_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.SAVINGS);

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
