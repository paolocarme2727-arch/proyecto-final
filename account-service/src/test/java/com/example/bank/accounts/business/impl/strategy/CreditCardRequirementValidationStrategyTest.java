package com.example.bank.accounts.business.impl.strategy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerProfile;
import com.example.bank.accounts.expose.model.CustomerType;
import com.example.bank.accounts.proxy.CreditProductProxy;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for credit card requirement validation.
 */
class CreditCardRequirementValidationStrategyTest {

    private final CreditProductProxy creditProductProxy = mock(CreditProductProxy.class);
    private final CreditCardRequirementValidationStrategy strategy =
            new CreditCardRequirementValidationStrategy(creditProductProxy);

    @Test
    void givenVipSavingsWithoutCreditCard_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("vip-1", CustomerType.PERSONAL, AccountType.SAVINGS)
                .customerProfile(CustomerProfile.VIP)
                .initialBalance(BigDecimal.valueOf(700));

        when(creditProductProxy.customerHasCreditCard("vip-1")).thenReturn(Single.just(Boolean.FALSE));

        strategy.validate(request)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
    }
}
