package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for VIP profile validation.
 */
class VipProfileValidationStrategyTest {

    private final VipProfileValidationStrategy strategy = new VipProfileValidationStrategy();

    @Test
    void givenBusinessVipAccount_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("business-1", CustomerType.BUSINESS, AccountType.CHECKING)
                .customerProfile(CustomerProfile.VIP)
                .holders(List.of("holder-1"));

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
