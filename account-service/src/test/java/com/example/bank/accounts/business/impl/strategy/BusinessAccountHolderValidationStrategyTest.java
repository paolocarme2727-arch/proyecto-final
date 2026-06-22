package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for business account holder validation.
 */
class BusinessAccountHolderValidationStrategyTest {

    private final BusinessAccountHolderValidationStrategy strategy = new BusinessAccountHolderValidationStrategy();

    @Test
    void givenBusinessAccountWithoutHolders_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.CHECKING);

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
