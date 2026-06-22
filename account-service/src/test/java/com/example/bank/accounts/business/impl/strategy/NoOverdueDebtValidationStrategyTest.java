package com.example.bank.accounts.business.impl.strategy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bank.accounts.events.CreditDebtStatusCache;
import com.example.bank.accounts.expose.model.AccountRequest;
import com.example.bank.accounts.expose.model.AccountType;
import com.example.bank.accounts.expose.model.CustomerType;
import io.reactivex.rxjava3.core.Single;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for overdue debt validation.
 */
class NoOverdueDebtValidationStrategyTest {

    private final CreditDebtStatusCache creditDebtStatusCache = mock(CreditDebtStatusCache.class);
    private final NoOverdueDebtValidationStrategy strategy =
            new NoOverdueDebtValidationStrategy(creditDebtStatusCache);

    @Test
    void givenCustomerWithOverdueDebt_whenValidate_thenRejectsRequest() {
        AccountRequest request = new AccountRequest("customer-1", CustomerType.PERSONAL, AccountType.SAVINGS);

        when(creditDebtStatusCache.hasOverdueDebt("customer-1")).thenReturn(Single.just(Boolean.TRUE));

        strategy.validate(request)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(ResponseStatusException.class);
    }
}
