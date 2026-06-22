package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for PYME profile validation.
 */
class PymeProfileValidationStrategyTest {

    private final PymeProfileValidationStrategy strategy = new PymeProfileValidationStrategy();

    @Test
    void givenPersonalPymeAccount_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.CHECKING)
                .customerProfile(CustomerProfile.PYME);

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
