package com.example.bank.accounts.business.impl.strategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bank.accounts.config.AccountProperties;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for VIP savings minimum average validation.
 */
class VipSavingsMinimumAverageValidationStrategyTest {

    private final AccountProperties properties = new AccountProperties(
            20,
            BigDecimal.valueOf(15),
            BigDecimal.ZERO,
            20,
            BigDecimal.valueOf(3.5),
            BigDecimal.valueOf(500));
    private final VipSavingsMinimumAverageValidationStrategy strategy =
            new VipSavingsMinimumAverageValidationStrategy(properties);

    @Test
    void givenVipSavingsBelowMinimumAverage_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("vip-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                .customerProfile(CustomerProfile.VIP)
                .initialBalance(BigDecimal.valueOf(499));

        assertThatThrownBy(() -> strategy.validate(request))
                .isInstanceOf(ResponseStatusException.class);
    }
}
